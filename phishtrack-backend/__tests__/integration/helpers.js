'use strict';

/**
 * Shared integration test helpers:
 *  - resetDatabase(): truncates all tables in dependency order
 *  - makeUser(): creates a user record and returns { user, token }
 */

const prisma = require('../../src/prismaClient');
const { signAccessToken } = require('../../src/utils/jwt.util');
const bcrypt = require('bcrypt');

/**
 * Truncate all tables in dependency-safe order.
 * Called in afterEach / beforeAll blocks.
 */
async function resetDatabase() {
  // Use raw SQL to truncate with cascade for speed
  await prisma.$executeRawUnsafe(
    `TRUNCATE TABLE "ChainOfCustody", "AuditLog", "Report", "Analysis", "Case", "User" CASCADE`
  );
}

/**
 * Create a test user and return their JWT and Prisma record.
 * @param {Partial<import('@prisma/client').User>} overrides
 */
async function makeUser(overrides = {}) {
  const password = await bcrypt.hash('Password123!', 8);
  const user = await prisma.user.create({
    data: {
      email: overrides.email ?? `test-${Date.now()}@example.com`,
      password,
      name: overrides.name ?? 'Test User',
      is_verified: true,
      role: overrides.role ?? 'analyst',
      ...overrides
    }
  });
  const token = signAccessToken({ userId: user.id });
  return { user, token };
}

module.exports = { resetDatabase, makeUser };
