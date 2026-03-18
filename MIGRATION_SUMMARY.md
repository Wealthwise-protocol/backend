# Fund Table Migration Summary

## Overview
Successfully migrated from old funds table schema to new schema with the following key changes:

### Schema Changes

#### Old Schema
- Primary Key: `id` (String/VARCHAR)
- Individual columns for returns (return_1y, return_3y, return_5y)
- Individual columns for category averages
- BigDecimal for min_sip and min_lumpsum
- BigDecimal for aum
- Had updated_at timestamp

#### New Schema
- Primary Key: `id` (UUID, auto-generated)
- New field: `scheme_code` (Integer, NOT NULL) - stores the actual MF API scheme code
- JSONB field: `returns` - stores all return periods as JSON
- JSONB field: `category_avg` - stores all category averages as JSON
- Integer for min_sip and min_lumpsum
- TEXT for aum
- Removed updated_at timestamp (only created_at remains)

## Files Modified

### 1. Entity Layer
- **Fund.java** - Complete restructure with UUID primary key, scheme_code, JSONB fields
- **Bookmark.java** - No changes (already using proper relationships)
- **FundNavHistory.java** - No changes (already using proper relationships)

### 2. Repository Layer
- **FundRepository.java** - Changed from `JpaRepository<Fund, String>` to `JpaRepository<Fund, UUID>`
  - Added `findBySchemeCode(Integer)` method
  - Added `existsBySchemeCode(Integer)` method
- **BookmarkRepository.java** - Changed fund_id type from String to UUID
- **FundNavHistoryRepository.java** - Changed fund_id type from String to UUID

### 3. DTO Layer
- **FundResponse.java** - Updated to match new entity structure
  - Changed id from String to UUID
  - Added schemeCode field
  - Replaced individual return fields with Map<String, BigDecimal> returns
  - Replaced individual categoryAvg fields with Map<String, BigDecimal> categoryAvg
  - Changed minSip/minLumpsum from BigDecimal to Integer
  - Changed aum from BigDecimal to String
- **BookmarkResponse.java** - Changed fundIds from List<String> to List<UUID>
- **FundDetailsRequest.java** - Changed id from String to UUID

### 4. Controller Layer
- **FundController.java** - Updated path variables from String to UUID
- **BookmarkController.java** - Updated path variables from String to UUID

### 5. Service Layer
- **FundService.java** - Major updates:
  - All fund ID parameters changed from String to UUID
  - Updated to use scheme_code for API calls
  - Updated toFundResponse() to map new fields
  - Updated invest() method to handle Integer min amounts
  - Updated fetchAndSaveFund() to use scheme_code
- **MfApiService.java** - No changes needed (already uses String scheme codes)

### 6. Configuration Layer
- **DataSeeder.java** - Updated to use Integer scheme codes
- **NavSyncJob.java** - Updated to use scheme_code for API calls
- **application.properties** - Changed `spring.jpa.hibernate.ddl-auto` from `update` to `create`

## Database Migration

### Important Notes
1. **Data Loss Warning**: Setting `ddl-auto=create` will DROP all existing tables and recreate them
2. All existing fund data, bookmarks, and NAV history will be lost
3. The DataSeeder will repopulate 9 popular funds on startup

### After First Successful Run
**IMPORTANT**: Change `spring.jpa.hibernate.ddl-auto` back to `update` in application.properties to prevent data loss on subsequent restarts.

## API Changes

### Request/Response Changes
- All fund IDs in API requests/responses are now UUIDs instead of scheme codes
- Fund response now includes `schemeCode` as a separate field
- Returns are now in a JSON object: `{"1Y": 18.4, "3Y": 17.2, "5Y": 22.1}`
- Category averages are now in a JSON object: `{"1Y": 19.8, "3Y": 15.4, "5Y": 18.2}`

### Endpoint Changes
- `GET /funds/{id}` - id is now UUID
- `GET /funds/{id}/nav-history` - id is now UUID
- `POST /funds/{id}/invest` - id is now UUID
- `POST /bookmarks/{fundId}` - fundId is now UUID
- `DELETE /bookmarks/{fundId}` - fundId is now UUID

## Frontend Impact

Frontend applications will need to:
1. Update all fund ID references from String to UUID
2. Handle the new `schemeCode` field if needed
3. Parse returns from JSON object instead of individual fields
4. Parse categoryAvg from JSON object instead of individual fields
5. Handle minSip/minLumpsum as integers instead of decimals
6. Handle aum as string instead of decimal

## Testing Checklist

- [ ] Application starts without errors
- [ ] Tables are created with correct schema
- [ ] DataSeeder successfully populates 9 funds
- [ ] Fund search works correctly
- [ ] Fund details retrieval works
- [ ] NAV history retrieval works
- [ ] Bookmarks can be added/removed
- [ ] Investment validation works
- [ ] NavSyncJob runs without errors

## Rollback Plan

If issues occur:
1. Change `spring.jpa.hibernate.ddl-auto` back to `update`
2. Restore database from backup
3. Revert all code changes using git

## Next Steps

1. Run the application and verify all tables are created correctly
2. Test all API endpoints
3. Change `ddl-auto` back to `update` after successful verification
4. Update frontend to handle new API structure
5. Consider adding database migration scripts for production deployments
