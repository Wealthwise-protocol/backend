# Frontend Integration Guide - Fund Table Migration

## Quick Reference for Frontend Developers

---

## 🔴 Breaking Changes

### 1. Fund ID is now UUID (not string scheme code)

**Before:**
```javascript
fundId: "122639"  // String scheme code
```

**After:**
```javascript
fundId: "99613e35-9ccd-4391-8f99-6b096ff7d0f7"  // UUID
```

**Impact**: All fund ID references must handle UUIDs

---

### 2. Returns are now in JSON object

**Before:**
```javascript
{
  return1y: 18.4,
  return3y: 17.2,
  return5y: 22.1
}
```

**After:**
```javascript
{
  returns: {
    "1Y": 18.4,
    "3Y": 17.2,
    "5Y": 22.1
  }
}
```

**Migration Code:**
```javascript
// Accessing returns
const return1y = fund.returns?.["1Y"] || null;
const return3y = fund.returns?.["3Y"] || null;
const return5y = fund.returns?.["5Y"] || null;
```

---

### 3. Category Averages are now in JSON object

**Before:**
```javascript
{
  categoryAvg1y: 19.8,
  categoryAvg3y: 15.4,
  categoryAvg5y: 18.2
}
```

**After:**
```javascript
{
  categoryAvg: {
    "1Y": 19.8,
    "3Y": 15.4,
    "5Y": 18.2
  }
}
```

**Migration Code:**
```javascript
// Accessing category averages
const categoryAvg1y = fund.categoryAvg?.["1Y"] || null;
const categoryAvg3y = fund.categoryAvg?.["3Y"] || null;
const categoryAvg5y = fund.categoryAvg?.["5Y"] || null;
```

---

### 4. Min Investment Amounts are now Integers

**Before:**
```javascript
{
  minSip: 500.00,      // BigDecimal
  minLumpsum: 5000.00  // BigDecimal
}
```

**After:**
```javascript
{
  minSip: 500,      // Integer
  minLumpsum: 5000  // Integer
}
```

**Migration Code:**
```javascript
// No code change needed, just remove decimal formatting
const minSip = fund.minSip; // Already an integer
```

---

### 5. AUM is now String (not number)

**Before:**
```javascript
{
  aum: 1234567890.00  // BigDecimal
}
```

**After:**
```javascript
{
  aum: "₹12,345 Cr"  // String (pre-formatted)
}
```

**Migration Code:**
```javascript
// Display directly, no formatting needed
const aum = fund.aum || "N/A";
```

---

### 6. New Field: schemeCode

**New Field:**
```javascript
{
  schemeCode: 122639  // Integer - original MF API code
}
```

**Usage:**
```javascript
// Use for display or reference if needed
const schemeCode = fund.schemeCode;
```

---

## 📋 Updated TypeScript Interfaces

### Fund Interface

```typescript
interface Fund {
  id: string;                              // UUID (changed from scheme code)
  schemeCode: number;                      // NEW: Original scheme code
  name: string;
  amc: string;
  category: string;
  subcategory: string;
  risk: string | null;
  description: string | null;
  nav: number;
  navChange: number | null;
  navChangePercent: number | null;
  aum: string | null;                      // Changed from number to string
  expenseRatio: number | null;
  minSip: number | null;                   // Changed from decimal to integer
  minLumpsum: number | null;               // Changed from decimal to integer
  returns: {                               // NEW: JSON object
    "1Y"?: number;
    "3Y"?: number;
    "5Y"?: number;
  } | null;
  categoryAvg: {                           // NEW: JSON object
    "1Y"?: number;
    "3Y"?: number;
    "5Y"?: number;
  } | null;
}
```

### Bookmark Response Interface

```typescript
interface BookmarkResponse {
  fundIds: string[];  // Array of UUIDs (changed from string scheme codes)
}
```

---

## 🔧 Migration Helper Functions

### 1. Safe Returns Access

```typescript
function getReturn(fund: Fund, period: "1Y" | "3Y" | "5Y"): number | null {
  return fund.returns?.[period] ?? null;
}

// Usage
const return1y = getReturn(fund, "1Y");
```

### 2. Safe Category Average Access

```typescript
function getCategoryAvg(fund: Fund, period: "1Y" | "3Y" | "5Y"): number | null {
  return fund.categoryAvg?.[period] ?? null;
}

// Usage
const categoryAvg1y = getCategoryAvg(fund, "1Y");
```

### 3. Format Min Investment

```typescript
function formatMinInvestment(amount: number | null): string {
  if (!amount) return "N/A";
  return `₹${amount.toLocaleString("en-IN")}`;
}

// Usage
const formattedMinSip = formatMinInvestment(fund.minSip);
```

### 4. Display AUM

```typescript
function displayAUM(aum: string | null): string {
  return aum || "N/A";
}

// Usage
const displayedAUM = displayAUM(fund.aum);
```

---

## 🔄 API Endpoint Changes

### Get Fund Details

**Before:**
```javascript
POST /funds
Body: { "id": "122639" }  // String scheme code
```

**After:**
```javascript
POST /funds
Body: { "id": "99613e35-9ccd-4391-8f99-6b096ff7d0f7" }  // UUID
```

### Get NAV History

**Before:**
```javascript
GET /funds/122639/nav-history?period=1Y
```

**After:**
```javascript
GET /funds/99613e35-9ccd-4391-8f99-6b096ff7d0f7/nav-history?period=1Y
```

### Bookmarks

**Before:**
```javascript
POST /bookmarks/122639    // String scheme code
DELETE /bookmarks/122639
```

**After:**
```javascript
POST /bookmarks/99613e35-9ccd-4391-8f99-6b096ff7d0f7    // UUID
DELETE /bookmarks/99613e35-9ccd-4391-8f99-6b096ff7d0f7
```

---

## 📝 Example Component Updates

### React Component Example

```typescript
// Before
const FundCard = ({ fund }) => {
  return (
    <div>
      <h3>{fund.name}</h3>
      <p>1Y Return: {fund.return1y}%</p>
      <p>3Y Return: {fund.return3y}%</p>
      <p>Min SIP: ₹{fund.minSip.toFixed(2)}</p>
      <p>AUM: ₹{(fund.aum / 10000000).toFixed(2)} Cr</p>
    </div>
  );
};

// After
const FundCard = ({ fund }) => {
  return (
    <div>
      <h3>{fund.name}</h3>
      <p>1Y Return: {fund.returns?.["1Y"] ?? "N/A"}%</p>
      <p>3Y Return: {fund.returns?.["3Y"] ?? "N/A"}%</p>
      <p>Min SIP: ₹{fund.minSip?.toLocaleString("en-IN") ?? "N/A"}</p>
      <p>AUM: {fund.aum ?? "N/A"}</p>
    </div>
  );
};
```

### API Service Example

```typescript
// Before
class FundService {
  async getFundDetails(schemeCode: string): Promise<Fund> {
    const response = await fetch(`/funds`, {
      method: 'POST',
      body: JSON.stringify({ id: schemeCode })
    });
    return response.json();
  }
}

// After
class FundService {
  async getFundDetails(fundId: string): Promise<Fund> {
    const response = await fetch(`/funds`, {
      method: 'POST',
      body: JSON.stringify({ id: fundId })  // Now expects UUID
    });
    return response.json();
  }
}
```

---

## ✅ Testing Checklist

- [ ] Update all fund ID references to handle UUIDs
- [ ] Update returns access to use JSON object
- [ ] Update category average access to use JSON object
- [ ] Remove decimal formatting from minSip/minLumpsum
- [ ] Update AUM display to show string directly
- [ ] Update all API calls to use UUID in URLs
- [ ] Update bookmark functionality to use UUIDs
- [ ] Test fund search functionality
- [ ] Test fund details page
- [ ] Test bookmark add/remove
- [ ] Test NAV history chart
- [ ] Update TypeScript interfaces
- [ ] Update unit tests
- [ ] Update integration tests

---

## 🐛 Common Issues & Solutions

### Issue 1: "Invalid UUID format" error

**Cause**: Trying to use old string scheme code as UUID

**Solution**: Ensure you're using the `id` field (UUID), not `schemeCode`

```typescript
// Wrong
const fundId = fund.schemeCode.toString();

// Correct
const fundId = fund.id;
```

### Issue 2: "Cannot read property '1Y' of null"

**Cause**: Accessing returns/categoryAvg without null check

**Solution**: Use optional chaining

```typescript
// Wrong
const return1y = fund.returns["1Y"];

// Correct
const return1y = fund.returns?.["1Y"] ?? null;
```

### Issue 3: Decimal places showing for minSip/minLumpsum

**Cause**: Still using toFixed() on integer values

**Solution**: Remove decimal formatting

```typescript
// Wrong
const minSip = fund.minSip.toFixed(2);

// Correct
const minSip = fund.minSip;
```

---

## 📞 Support

If you encounter any issues during migration:

1. Check the test script: `./test-api.sh`
2. Review the complete migration guide: `MIGRATION_COMPLETE.md`
3. Test endpoints using the provided examples
4. Contact backend team for API-related issues

---

## 🎯 Summary

**Key Changes:**
1. Fund IDs are now UUIDs (longer strings)
2. Returns and categoryAvg are JSON objects
3. minSip/minLumpsum are integers
4. AUM is a pre-formatted string
5. New schemeCode field available

**Migration Effort:** Low to Medium
**Estimated Time:** 2-4 hours for a typical frontend

**Status:** Backend migration complete ✅
**Next Step:** Update frontend code

---

**Last Updated**: March 18, 2026
**Backend Version**: v2.0
**API Base URL**: http://localhost:9095
