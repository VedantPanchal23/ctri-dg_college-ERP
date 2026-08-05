export const ROLES = {
  PLATFORM_SUPER_ADMIN: 'PLATFORM_SUPER_ADMIN',
  TENANT_ADMIN: 'TENANT_ADMIN',
  ACADEMIC_ADMIN: 'ACADEMIC_ADMIN',
  EXAM_CONTROLLER: 'EXAM_CONTROLLER',
  FACULTY: 'FACULTY',
  HOD: 'HOD',
  STUDENT: 'STUDENT',
  PLACEMENT_OFFICER: 'PLACEMENT_OFFICER',
  RECRUITER: 'RECRUITER',
}

export function hasAnyRole(userRoles, allowed) {
  if (!userRoles?.length || !allowed?.length) return false
  return allowed.some((r) => userRoles.includes(r))
}

/** Navigation items visible by role */
export const NAV_ITEMS = [
  {
    to: '/app',
    label: 'Home',
    roles: Object.values(ROLES),
  },
  {
    to: '/app/platform/tenants',
    label: 'Tenants',
    roles: [ROLES.PLATFORM_SUPER_ADMIN],
  },
  {
    to: '/app/users',
    label: 'Users',
    roles: [ROLES.PLATFORM_SUPER_ADMIN, ROLES.TENANT_ADMIN, ROLES.ACADEMIC_ADMIN, ROLES.PLACEMENT_OFFICER],
  },
  {
    to: '/app/academic',
    label: 'Academic',
    roles: [
      ROLES.TENANT_ADMIN,
      ROLES.ACADEMIC_ADMIN,
      ROLES.EXAM_CONTROLLER,
      ROLES.FACULTY,
      ROLES.HOD,
      ROLES.PLACEMENT_OFFICER,
      ROLES.STUDENT,
    ],
  },
  {
    to: '/app/exams',
    label: 'Exams',
    roles: [
      ROLES.TENANT_ADMIN,
      ROLES.EXAM_CONTROLLER,
      ROLES.FACULTY,
      ROLES.HOD,
      ROLES.STUDENT,
    ],
  },
  {
    to: '/app/placements',
    label: 'Placements',
    roles: [
      ROLES.TENANT_ADMIN,
      ROLES.PLACEMENT_OFFICER,
      ROLES.RECRUITER,
      ROLES.STUDENT,
    ],
  },
  {
    to: '/app/recruiter',
    label: 'Recruiter',
    roles: [ROLES.RECRUITER, ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN],
  },
  {
    to: '/app/settings',
    label: 'Settings',
    roles: [ROLES.TENANT_ADMIN],
  },
  {
    to: '/app/audit',
    label: 'Audit log',
    roles: [ROLES.PLATFORM_SUPER_ADMIN, ROLES.TENANT_ADMIN],
  },
  {
    to: '/app/notifications',
    label: 'Notifications',
    roles: Object.values(ROLES),
  },
]

export function homeCardsForRoles(roles) {
  const cards = []
  if (hasAnyRole(roles, [ROLES.PLATFORM_SUPER_ADMIN])) {
    cards.push({
      title: 'Platform',
      text: 'Create and suspend college tenants.',
      to: '/app/platform/tenants',
    })
  }
  if (
    hasAnyRole(roles, [
      ROLES.TENANT_ADMIN,
      ROLES.ACADEMIC_ADMIN,
      ROLES.FACULTY,
      ROLES.STUDENT,
      ROLES.HOD,
      ROLES.EXAM_CONTROLLER,
      ROLES.PLACEMENT_OFFICER,
    ])
  ) {
    cards.push({
      title: 'Academic',
      text: 'Programs, courses, students, and enrollments.',
      to: '/app/academic',
    })
  }
  if (hasAnyRole(roles, [ROLES.TENANT_ADMIN, ROLES.EXAM_CONTROLLER, ROLES.FACULTY, ROLES.STUDENT, ROLES.HOD])) {
    cards.push({
      title: 'Exams',
      text: 'Sessions, schedules, hall tickets, seats, and marks.',
      to: '/app/exams',
    })
  }
  if (hasAnyRole(roles, [ROLES.TENANT_ADMIN, ROLES.PLACEMENT_OFFICER, ROLES.RECRUITER, ROLES.STUDENT])) {
    cards.push({
      title: 'Placements',
      text: 'Companies, drives, applications, and offers.',
      to: '/app/placements',
    })
  }
  if (hasAnyRole(roles, [ROLES.RECRUITER, ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN])) {
    cards.push({
      title: 'Recruiter',
      text: 'Open drives, application pipeline, rounds, and offers.',
      to: '/app/recruiter',
    })
  }
  if (hasAnyRole(roles, [ROLES.TENANT_ADMIN, ROLES.ACADEMIC_ADMIN, ROLES.PLATFORM_SUPER_ADMIN, ROLES.PLACEMENT_OFFICER])) {
    cards.push({
      title: 'Users',
      text: 'Link accounts and assign roles for your college.',
      to: '/app/users',
    })
  }
  if (hasAnyRole(roles, [ROLES.TENANT_ADMIN])) {
    cards.push({
      title: 'Settings',
      text: 'Edit college profile and bootstrap your academic structure.',
      to: '/app/settings',
    })
  }
  if (hasAnyRole(roles, [ROLES.PLATFORM_SUPER_ADMIN, ROLES.TENANT_ADMIN])) {
    cards.push({
      title: 'Audit log',
      text: 'Review recent administrative and data-changing actions.',
      to: '/app/audit',
    })
  }
  cards.push({
    title: 'Notifications',
    text: 'See updates relevant to your account.',
    to: '/app/notifications',
  })
  return cards
}

export function roleLabel(role) {
  return role.replaceAll('_', ' ')
}
