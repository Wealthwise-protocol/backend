#!/bin/bash

# WealthWise Backend API Test Script (auth + protected APIs)

BASE_URL="http://localhost:9095"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "=========================================="
echo "WealthWise Backend API Tests"
echo "=========================================="
echo ""

request() {
  local method="$1"
  local url="$2"
  local body="$3"
  local token="$4"

  if [ -n "$token" ]; then
    if [ -n "$body" ]; then
      curl -s -w "\n%{http_code}" -X "$method" "$url" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $token" \
        -d "$body"
    else
      curl -s -w "\n%{http_code}" -X "$method" "$url" \
        -H "Authorization: Bearer $token"
    fi
  else
    if [ -n "$body" ]; then
      curl -s -w "\n%{http_code}" -X "$method" "$url" \
        -H "Content-Type: application/json" \
        -d "$body"
    else
      curl -s -w "\n%{http_code}" -X "$method" "$url"
    fi
  fi
}

extract_body() {
  echo "$1" | sed '$d'
}

extract_code() {
  echo "$1" | tail -n1
}

# Test 1: Signup
echo -e "${YELLOW}Test 1: POST /auth/signup${NC}"
TEST_EMAIL="test_$(date +%s)@example.com"
TEST_PHONE="9$(date +%s | tail -c 10)"
SIGNUP_PAYLOAD="{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"$TEST_EMAIL\",\"phone\":\"$TEST_PHONE\",\"countryCode\":\"+91\",\"password\":\"Test@1234\"}"
RESP=$(request "POST" "$BASE_URL/auth/signup" "$SIGNUP_PAYLOAD" "")
BODY=$(extract_body "$RESP")
CODE=$(extract_code "$RESP")

TOKEN=""
if [ "$CODE" = "201" ]; then
  TOKEN=$(echo "$BODY" | jq -r '.token // empty')
  USER_ID=$(echo "$BODY" | jq -r '.user.id // empty')
  if [ -n "$TOKEN" ] && [ -n "$USER_ID" ]; then
    echo -e "${GREEN}✓ PASS${NC} - User created successfully"
    echo "  User ID: $USER_ID"
    echo "  Token: ${TOKEN:0:20}..."
  else
    echo -e "${RED}✗ FAIL${NC} - Signup response missing token/user"
    echo "$BODY" | jq '.'
    exit 1
  fi
else
  echo -e "${RED}✗ FAIL${NC} - Signup failed (HTTP $CODE)"
  echo "$BODY" | jq '.'
  exit 1
fi
echo ""

# Test 2: Get all funds (paginated + auth)
echo -e "${YELLOW}Test 2: GET /funds?page=0&size=10${NC}"
RESP=$(request "GET" "$BASE_URL/funds?page=0&size=10" "" "$TOKEN")
BODY=$(extract_body "$RESP")
CODE=$(extract_code "$RESP")

if [ "$CODE" != "200" ]; then
  echo -e "${RED}✗ FAIL${NC} - Funds fetch failed (HTTP $CODE)"
  echo "$BODY" | jq '.'
  exit 1
fi

FUND_COUNT=$(echo "$BODY" | jq -r '.content | length')
if [ "$FUND_COUNT" -gt 0 ]; then
  echo -e "${GREEN}✓ PASS${NC} - Found $FUND_COUNT fund(s)"
  FUND_ID=$(echo "$BODY" | jq -r '.content[0].id')
  SCHEME_CODE=$(echo "$BODY" | jq -r '.content[0].schemeCode')
  echo "  Sample Fund ID: $FUND_ID"
  echo "  Sample Scheme Code: $SCHEME_CODE"
else
  echo -e "${RED}✗ FAIL${NC} - No funds found"
  exit 1
fi
echo ""

# Test 3: Search funds
echo -e "${YELLOW}Test 3: GET /funds?search=parag&page=0&size=10${NC}"
RESP=$(request "GET" "$BASE_URL/funds?search=parag&page=0&size=10" "" "$TOKEN")
BODY=$(extract_body "$RESP")
CODE=$(extract_code "$RESP")

if [ "$CODE" = "200" ]; then
  SEARCH_COUNT=$(echo "$BODY" | jq -r '.content | length')
  if [ "$SEARCH_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓ PASS${NC} - Found $SEARCH_COUNT fund(s) matching 'parag'"
  else
    echo -e "${YELLOW}⚠ WARN${NC} - Search returned 0 results"
  fi
else
  echo -e "${RED}✗ FAIL${NC} - Search failed (HTTP $CODE)"
fi
echo ""

# Test 4: Get fund details by UUID
echo -e "${YELLOW}Test 4: POST /funds (Get fund details)${NC}"
RESP=$(request "POST" "$BASE_URL/funds" "{\"id\": \"$FUND_ID\"}" "$TOKEN")
BODY=$(extract_body "$RESP")
CODE=$(extract_code "$RESP")
FUND_NAME=$(echo "$BODY" | jq -r '.name // empty')
if [ "$CODE" = "200" ] && [ -n "$FUND_NAME" ]; then
  echo -e "${GREEN}✓ PASS${NC} - Retrieved fund: $FUND_NAME"
  echo "$BODY" | jq '{id, schemeCode, name, amc, category, nav}'
else
  echo -e "${RED}✗ FAIL${NC} - Could not retrieve fund details (HTTP $CODE)"
fi
echo ""

# Test 5: Get NAV history
echo -e "${YELLOW}Test 5: GET /funds/{id}/nav-history${NC}"
RESP=$(request "GET" "$BASE_URL/funds/$FUND_ID/nav-history?period=1Y" "" "$TOKEN")
BODY=$(extract_body "$RESP")
CODE=$(extract_code "$RESP")
NAV_COUNT=$(echo "$BODY" | jq -r '.data | length')
if [ "$CODE" = "200" ] && [ "$NAV_COUNT" -ge 0 ]; then
  echo -e "${GREEN}✓ PASS${NC} - NAV history endpoint working (found $NAV_COUNT records)"
else
  echo -e "${RED}✗ FAIL${NC} - NAV history endpoint error (HTTP $CODE)"
fi
echo ""

# Test 6: Get current user
echo -e "${YELLOW}Test 6: GET /auth/me${NC}"
RESP=$(request "GET" "$BASE_URL/auth/me" "" "$TOKEN")
BODY=$(extract_body "$RESP")
CODE=$(extract_code "$RESP")
USER_EMAIL=$(echo "$BODY" | jq -r '.user.email // empty')
if [ "$CODE" = "200" ] && [ "$USER_EMAIL" = "$TEST_EMAIL" ]; then
  echo -e "${GREEN}✓ PASS${NC} - Retrieved current user: $USER_EMAIL"
else
  echo -e "${RED}✗ FAIL${NC} - Could not retrieve current user (HTTP $CODE)"
fi
echo ""

# Test 7: Add bookmark
echo -e "${YELLOW}Test 7: POST /bookmarks/{fundId}${NC}"
RESP=$(request "POST" "$BASE_URL/bookmarks/$FUND_ID" "" "$TOKEN")
BODY=$(extract_body "$RESP")
CODE=$(extract_code "$RESP")
SUCCESS=$(echo "$BODY" | jq -r '.success // false')
if [ "$CODE" = "200" ] && [ "$SUCCESS" = "true" ]; then
  echo -e "${GREEN}✓ PASS${NC} - Bookmark added successfully"
else
  echo -e "${RED}✗ FAIL${NC} - Could not add bookmark (HTTP $CODE)"
fi
echo ""

# Test 8: Get bookmarks
echo -e "${YELLOW}Test 8: GET /bookmarks${NC}"
RESP=$(request "GET" "$BASE_URL/bookmarks" "" "$TOKEN")
BODY=$(extract_body "$RESP")
CODE=$(extract_code "$RESP")
BOOKMARK_COUNT=$(echo "$BODY" | jq -r '.fundIds | length')
if [ "$CODE" = "200" ]; then
  echo -e "${GREEN}✓ PASS${NC} - Found $BOOKMARK_COUNT bookmark(s)"
  echo "$BODY" | jq '.fundIds'
else
  echo -e "${RED}✗ FAIL${NC} - Could not fetch bookmarks (HTTP $CODE)"
fi
echo ""

# Test 9: Remove bookmark
echo -e "${YELLOW}Test 9: DELETE /bookmarks/{fundId}${NC}"
RESP=$(request "DELETE" "$BASE_URL/bookmarks/$FUND_ID" "" "$TOKEN")
BODY=$(extract_body "$RESP")
CODE=$(extract_code "$RESP")
SUCCESS=$(echo "$BODY" | jq -r '.success // false')
if [ "$CODE" = "200" ] && [ "$SUCCESS" = "true" ]; then
  echo -e "${GREEN}✓ PASS${NC} - Bookmark removed successfully"
else
  echo -e "${RED}✗ FAIL${NC} - Could not remove bookmark (HTTP $CODE)"
fi
echo ""

# Test 10: Verify schema fields on first fund
echo -e "${YELLOW}Test 10: Verify fund schema fields${NC}"
FIRST_FUND=$(echo "$BODY" >/dev/null; echo "")
RESP=$(request "GET" "$BASE_URL/funds?page=0&size=1" "" "$TOKEN")
BODY=$(extract_body "$RESP")
FIRST_FUND=$(echo "$BODY" | jq '.content[0]')

ID_TYPE=$(echo "$FIRST_FUND" | jq -r '.id' | grep -E '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' || true)
if [ -n "$ID_TYPE" ]; then
  echo -e "${GREEN}✓ PASS${NC} - ID is UUID format"
else
  echo -e "${RED}✗ FAIL${NC} - ID is not UUID format"
fi

SCHEME_CODE=$(echo "$FIRST_FUND" | jq -r '.schemeCode')
if [ "$SCHEME_CODE" != "null" ] && [ "$SCHEME_CODE" -gt 0 ]; then
  echo -e "${GREEN}✓ PASS${NC} - schemeCode field exists and is integer"
else
  echo -e "${RED}✗ FAIL${NC} - schemeCode field missing or invalid"
fi

RETURNS_TYPE=$(echo "$FIRST_FUND" | jq -r '.returns | type')
if [ "$RETURNS_TYPE" = "null" ] || [ "$RETURNS_TYPE" = "object" ]; then
  echo -e "${GREEN}✓ PASS${NC} - returns field is null or object"
else
  echo -e "${RED}✗ FAIL${NC} - returns field has wrong type"
fi

CATEGORY_AVG_TYPE=$(echo "$FIRST_FUND" | jq -r '.categoryAvg | type')
if [ "$CATEGORY_AVG_TYPE" = "null" ] || [ "$CATEGORY_AVG_TYPE" = "object" ]; then
  echo -e "${GREEN}✓ PASS${NC} - categoryAvg field is null or object"
else
  echo -e "${RED}✗ FAIL${NC} - categoryAvg field has wrong type"
fi

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo -e "${GREEN}Script completed.${NC}"
