import json, urllib.request, urllib.parse, ssl

KC = "http://localhost:8081"
REALM = "college-admin"
IIITB = "00000000-0000-0000-0000-000000000001"
API = "http://localhost:8080"

def post_form(url, data):
    body = urllib.parse.urlencode(data).encode()
    req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/x-www-form-urlencoded"})
    with urllib.request.urlopen(req) as r:
        return json.load(r)

def req(method, url, token=None, payload=None):
    data = None if payload is None else json.dumps(payload).encode()
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as r:
            raw = r.read()
            return r.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            body = json.loads(raw) if raw else None
        except Exception:
            body = raw.decode("utf-8", "ignore")
        return e.code, body

admin = post_form(f"{KC}/realms/master/protocol/openid-connect/token", {
    "grant_type": "password", "client_id": "admin-cli", "username": "admin", "password": "admin"
})["access_token"]

# Enable unmanaged attrs + declare tenant_id
status, profile = req("GET", f"{KC}/admin/realms/{REALM}/users/profile", admin)
assert status == 200, profile
attrs = profile.get("attributes") or []
if not any(a.get("name") == "tenant_id" for a in attrs):
    attrs.append({
        "name": "tenant_id",
        "displayName": "Tenant ID",
        "permissions": {"view": ["admin"], "edit": ["admin"]},
        "multivalued": False,
    })
profile["attributes"] = attrs
profile["unmanagedAttributePolicy"] = "ENABLED"
status, _ = req("PUT", f"{KC}/admin/realms/{REALM}/users/profile", admin, profile)
print("user profile update", status)

demos = [
    ("superadmin", "superadmin@platform.local", "Platform", "Admin", "SuperAdmin@123", ["PLATFORM_SUPER_ADMIN"], None),
    ("tenantadmin", "admin@iiitb.ac.in", "Tenant", "Admin", "TenantAdmin@123", ["TENANT_ADMIN"], IIITB),
    ("examcontroller", "exam@iiitb.ac.in", "Exam", "Controller", "Exam@123", ["EXAM_CONTROLLER"], IIITB),
    ("placement", "placement@iiitb.ac.in", "Placement", "Officer", "Placement@123", ["PLACEMENT_OFFICER"], IIITB),
    ("recruiter1", "recruiter1@iiitb.ac.in", "Campus", "Recruiter", "Recruiter@123", ["RECRUITER"], IIITB),
    ("faculty1", "faculty1@iiitb.ac.in", "Faculty", "One", "Faculty@123", ["FACULTY"], IIITB),
    ("student1", "student1@iiitb.ac.in", "Student", "One", "Student@123", ["STUDENT"], IIITB),
]

status, roles = req("GET", f"{KC}/admin/realms/{REALM}/roles", admin)
role_by_name = {r["name"]: r for r in roles}

fail = 0
for username, email, first, last, password, realm_roles, tenant in demos:
    status, users = req("GET", f"{KC}/admin/realms/{REALM}/users?username={urllib.parse.quote(username)}&exact=true", admin)
    if not users:
        payload = {
            "username": username, "email": email, "enabled": True, "emailVerified": True,
            "firstName": first, "lastName": last, "requiredActions": [],
        }
        if tenant:
            payload["attributes"] = {"tenant_id": [tenant]}
        status, _ = req("POST", f"{KC}/admin/realms/{REALM}/users", admin, payload)
        print("create", username, status)
        status, users = req("GET", f"{KC}/admin/realms/{REALM}/users?username={urllib.parse.quote(username)}&exact=true", admin)
    uid = users[0]["id"]
    payload = {
        "id": uid, "username": username, "email": email, "enabled": True, "emailVerified": True,
        "firstName": first, "lastName": last, "requiredActions": [],
        "attributes": {"tenant_id": [tenant]} if tenant else {},
    }
    status, _ = req("PUT", f"{KC}/admin/realms/{REALM}/users/{uid}", admin, payload)
    print("patch", username, status)
    status, _ = req("PUT", f"{KC}/admin/realms/{REALM}/users/{uid}/reset-password", admin, {
        "type": "password", "value": password, "temporary": False
    })
    to_assign = [{"id": role_by_name[r]["id"], "name": r} for r in realm_roles if r in role_by_name]
    if to_assign:
        req("POST", f"{KC}/admin/realms/{REALM}/users/{uid}/role-mappings/realm", admin, to_assign)
    # verify attrs
    status, u = req("GET", f"{KC}/admin/realms/{REALM}/users/{uid}", admin)
    attrs = (u or {}).get("attributes") or {}
    print("  attrs", attrs)

print("\n=== login + tenant claim ===")
for username, email, first, last, password, realm_roles, tenant in demos:
    try:
        tok = post_form(f"{KC}/realms/{REALM}/protocol/openid-connect/token", {
            "grant_type": "password", "client_id": "college-admin-api",
            "client_secret": "college-admin-api-secret", "username": username, "password": password
        })["access_token"]
    except Exception as e:
        print("FAIL login", username, e)
        fail += 1
        continue
    import base64
    payload = tok.split(".")[1]
    payload += "=" * (-len(payload) % 4)
    claims = json.loads(base64.urlsafe_b64decode(payload.encode()))
    has = "tenant_id" in claims
    print(f"PASS login {username} tenant_claim={claims.get('tenant_id')}")
    if tenant and not has:
        print("FAIL missing tenant_id claim for", username)
        fail += 1

# Sync local recruiter tenant + company via rebuilt filter after login
print("\n=== link recruiter company ===")
rec_tok = post_form(f"{KC}/realms/{REALM}/protocol/openid-connect/token", {
    "grant_type": "password", "client_id": "college-admin-api",
    "client_secret": "college-admin-api-secret", "username": "recruiter1", "password": "Recruiter@123"
})["access_token"]
# Need app rebuild for filter - for now use SQL via mysql? Or platform admin update
# Fix local DB tenant via calling me after filter fix; for now use tenantadmin list and platform

admin_tok = post_form(f"{KC}/realms/{REALM}/protocol/openid-connect/token", {
    "grant_type": "password", "client_id": "college-admin-api",
    "client_secret": "college-admin-api-secret", "username": "tenantadmin", "password": "TenantAdmin@123"
})["access_token"]
place_tok = post_form(f"{KC}/realms/{REALM}/protocol/openid-connect/token", {
    "grant_type": "password", "client_id": "college-admin-api",
    "client_secret": "college-admin-api-secret", "username": "placement", "password": "Placement@123"
})["access_token"]

status, me = req("GET", f"{API}/api/v1/users/me", rec_tok)
print("recruiter me", status, me)
# If tenant still null, update via MySQL docker
if me and me.get("tenantId") is None:
    import subprocess
    sql = f"UPDATE user_accounts SET tenant_id=UUID_TO_BIN('{IIITB}') WHERE email='recruiter1@iiitb.ac.in';"
    subprocess.run(["docker", "exec", "ca-mysql", "mysql", "-uca_user", "-pca_pass", "college_admin", "-e", sql], check=False)
    status, me = req("GET", f"{API}/api/v1/users/me", rec_tok)
    print("recruiter me after sql", status, me)

status, cos = req("GET", f"{API}/api/v1/placements/companies?page=0&size=50", place_tok)
company = None
if cos and cos.get("content"):
    company = next((c for c in cos["content"] if c.get("code") == "DEMOREC"), cos["content"][0])
if not company:
    status, company = req("POST", f"{API}/api/v1/placements/companies", place_tok, {
        "name": "Demo Recruiter Co", "code": "DEMOREC", "contactEmail": "hr@demorec.test"
    })
print("company", company)
if me and company:
    status, linked = req("PUT", f"{API}/api/v1/users/{me['id']}/company", admin_tok, {"companyId": company["id"]})
    print("link company", status, linked)
    if status != 200:
        fail += 1

print("\nRESULT failures=", fail)
raise SystemExit(1 if fail else 0)
