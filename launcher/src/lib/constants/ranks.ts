// Simple owner detection for permission checks

// Owner UUID (hardcoded for now - can be moved to backend later)
const OWNER_UUID = 'e3457d416c3b47219ccb05fa90369eeb';

/**
 * Check if a user is the owner by UUID
 */
export function getAccountRank(uuid: string): 'owner' | 'standard' {
  const cleanUuid = uuid.replace(/-/g, '').toLowerCase();
  return cleanUuid === OWNER_UUID ? 'owner' : 'standard';
}
