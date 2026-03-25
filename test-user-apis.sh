#!/bin/bash

# WealthWise User Module API Test Suite
# Tests all 9 authentication endpoints

BASE_URL="https://wealthwise-backend.onrender.com"
# BASE_URL="http://localhost:9095"  # Uncomment for local testing

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Variables to store data between tests
TOKEN=""
USER_ID=""
TEST_EMAIL="test_$(date +%s)@example.com"
TEST_PASSWORD="TestPass123!"
NEW_PASSWORD="NewPass456!"
RESET_OTP=""

echo "=========================================="
echo "  WealthWise User Module API Test Suite"
echo "=========================================="
echo "Base URL: $BASE_URL"
echo "Test Email: $TEST_EMAIL"
echo "=========================================="
echo ""

# Helper function to print test results
print_result() {
    local test_name=$1
    local status=$2
    local response=$3
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$status" == "PASS" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo -e "${GREEN}✅ PASS${NC} | $test_name"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "${RED}❌ FAIL${NC} | $test_name"
    fi
    
    if [ ! -z "$response" ]; then
        echo "   Response: $response"
    fi
    echo ""
}

# Helper function to make API calls
api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    local auth_header=$4
    
    if [ ! -z "$auth_header" ]; then
        curl -s -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $auth_header" \
            -d "$data" \
            -w "\n%{http_code}"
    else
        curl -s -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data" \
            -w "\n%{http_code}"
    fi
}

echo "=========================================="
echo "TEST 1: Sign Up (POST /auth/signup)"
echo "=========================================="

RESPONSE=$(api_call POST "/auth/signup" '{
  "firstName": "Test",
  "lastName": "User",
  "email": "'"$TEST_EMAIL"'",
  "phone": "9876543210",
  "countryCode": "+91",
  "password": "'"$TEST_PASSWORD"'"
}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "201" ]; then
    TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    USER_ID=$(echo "$BODY" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
    print_result "Sign Up - New User" "PASS" "User created with ID: $USER_ID"
else
    print_result "Sign Up - New User" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 2: Sign Up - Duplicate Email"
echo "=========================================="

RESPONSE=$(api_call POST "/auth/signup" '{
  "firstName": "Test",
  "lastName": "User",
  "email": "'"$TEST_EMAIL"'",
  "phone": "9876543211",
  "password": "'"$TEST_PASSWORD"'"
}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "409" ]; then
    print_result "Sign Up - Duplicate Email (409 Expected)" "PASS" "Correctly rejected duplicate email"
else
    print_result "Sign Up - Duplicate Email (409 Expected)" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 3: Sign In (POST /auth/signin)"
echo "=========================================="

RESPONSE=$(api_call POST "/auth/signin" '{
  "email": "'"$TEST_EMAIL"'",
  "password": "'"$TEST_PASSWORD"'"
}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    print_result "Sign In - Valid Credentials" "PASS" "Token received"
else
    print_result "Sign In - Valid Credentials" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 4: Sign In - Invalid Password"
echo "=========================================="

RESPONSE=$(api_call POST "/auth/signin" '{
  "email": "'"$TEST_EMAIL"'",
  "password": "WrongPassword123!"
}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "401" ]; then
    print_result "Sign In - Invalid Password (401 Expected)" "PASS" "Correctly rejected wrong password"
else
    print_result "Sign In - Invalid Password (401 Expected)" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 5: Get Current User (GET /auth/me)"
echo "=========================================="

RESPONSE=$(api_call GET "/auth/me" "" "$TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    print_result "Get Current User - With Token" "PASS" "User data retrieved"
else
    print_result "Get Current User - With Token" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 6: Get Current User - No Token"
echo "=========================================="

RESPONSE=$(curl -s -X GET "$BASE_URL/auth/me" -w "\n%{http_code}")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "401" ]; then
    print_result "Get Current User - No Token (401 Expected)" "PASS" "Correctly rejected unauthorized request"
else
    print_result "Get Current User - No Token (401 Expected)" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 7: Update Profile (PATCH /auth/profile)"
echo "=========================================="

RESPONSE=$(api_call PATCH "/auth/profile" '{
  "firstName": "Updated",
  "lastName": "Name",
  "phone": "9876543210"
}' "$TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    print_result "Update Profile - Valid Data" "PASS" "Profile updated successfully"
else
    print_result "Update Profile - Valid Data" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 8: Change Password (POST /auth/change-password)"
echo "=========================================="

RESPONSE=$(api_call POST "/auth/change-password" '{
  "currentPassword": "'"$TEST_PASSWORD"'",
  "newPassword": "'"$NEW_PASSWORD"'"
}' "$TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    TEST_PASSWORD="$NEW_PASSWORD"
    print_result "Change Password - Valid Current Password" "PASS" "Password changed successfully"
else
    print_result "Change Password - Valid Current Password" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 9: Change Password - Wrong Current Password"
echo "=========================================="

RESPONSE=$(api_call POST "/auth/change-password" '{
  "currentPassword": "WrongPassword123!",
  "newPassword": "AnotherPass789!"
}' "$TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "401" ]; then
    print_result "Change Password - Wrong Current (401 Expected)" "PASS" "Correctly rejected wrong password"
else
    print_result "Change Password - Wrong Current (401 Expected)" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 10: Sign In After Password Change"
echo "=========================================="

RESPONSE=$(api_call POST "/auth/signin" '{
  "email": "'"$TEST_EMAIL"'",
  "password": "'"$TEST_PASSWORD"'"
}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    print_result "Sign In - After Password Change" "PASS" "Login successful with new password"
else
    print_result "Sign In - After Password Change" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 11: Forgot Password (POST /auth/forgot-password)"
echo "=========================================="

RESPONSE=$(api_call POST "/auth/forgot-password" '{
  "email": "'"$TEST_EMAIL"'"
}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    print_result "Forgot Password - Valid Email" "PASS" "OTP sent (check email)"
    echo -e "${YELLOW}⚠️  Manual Step Required: Check email for OTP and enter below${NC}"
    read -p "Enter OTP from email: " RESET_OTP
else
    print_result "Forgot Password - Valid Email" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

if [ ! -z "$RESET_OTP" ]; then
    echo "=========================================="
    echo "TEST 12: Reset Password (POST /auth/reset-password)"
    echo "=========================================="

    RESPONSE=$(api_call POST "/auth/reset-password" '{
      "otp": "'"$RESET_OTP"'",
      "newPassword": "ResetPass999!"
    }')

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" == "200" ]; then
        TEST_PASSWORD="ResetPass999!"
        print_result "Reset Password - Valid OTP" "PASS" "Password reset successfully"
    else
        print_result "Reset Password - Valid OTP" "FAIL" "HTTP $HTTP_CODE: $BODY"
    fi

    echo "=========================================="
    echo "TEST 13: Sign In After Password Reset"
    echo "=========================================="

    RESPONSE=$(api_call POST "/auth/signin" '{
      "email": "'"$TEST_EMAIL"'",
      "password": "'"$TEST_PASSWORD"'"
    }')

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" == "200" ]; then
        TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        print_result "Sign In - After Password Reset" "PASS" "Login successful with reset password"
    else
        print_result "Sign In - After Password Reset" "FAIL" "HTTP $HTTP_CODE: $BODY"
    fi
else
    echo -e "${YELLOW}⚠️  Skipping Reset Password tests (no OTP provided)${NC}"
    echo ""
fi

echo "=========================================="
echo "TEST 14: Reset Password - Invalid OTP"
echo "=========================================="

RESPONSE=$(api_call POST "/auth/reset-password" '{
  "otp": "999999",
  "newPassword": "TestPass123!"
}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "400" ]; then
    print_result "Reset Password - Invalid OTP (400 Expected)" "PASS" "Correctly rejected invalid OTP"
else
    print_result "Reset Password - Invalid OTP (400 Expected)" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 15: Sign Out (POST /auth/signout)"
echo "=========================================="

RESPONSE=$(api_call POST "/auth/signout" "" "$TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    print_result "Sign Out - With Token" "PASS" "Signed out successfully"
else
    print_result "Sign Out - With Token" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 16: Delete Account (DELETE /auth/account)"
echo "=========================================="

RESPONSE=$(api_call DELETE "/auth/account" "" "$TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    print_result "Delete Account - With Token" "PASS" "Account deleted successfully"
else
    print_result "Delete Account - With Token" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo "=========================================="
echo "TEST 17: Sign In After Account Deletion"
echo "=========================================="

RESPONSE=$(api_call POST "/auth/signin" '{
  "email": "'"$TEST_EMAIL"'",
  "password": "'"$TEST_PASSWORD"'"
}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "401" ]; then
    print_result "Sign In - After Deletion (401 Expected)" "PASS" "Correctly rejected deleted account"
else
    print_result "Sign In - After Deletion (401 Expected)" "FAIL" "HTTP $HTTP_CODE: $BODY"
fi

echo ""
echo "=========================================="
echo "           TEST SUMMARY"
echo "=========================================="
echo -e "Total Tests:  ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed:       ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed:       ${RED}$FAILED_TESTS${NC}"
echo "=========================================="

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ ALL TESTS PASSED!${NC}"
    exit 0
else
    echo -e "${RED}❌ SOME TESTS FAILED${NC}"
    exit 1
fi
