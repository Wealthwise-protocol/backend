#!/bin/bash

# ============================================================
# WealthWise — Push feature/user-auth to GitHub Org repo
# Usage: chmod +x push-feature.sh && ./push-feature.sh
# ============================================================

GITHUB_ORG="Wealthwise-protocol"
REPO_NAME="backend"
BRANCH_NAME="feature/user-auth-module"

echo "🔧 Initializing git..."
git init

echo "📦 Staging all files..."
git add .

echo "✍️  Committing..."
git commit -m "feat(auth): add user authentication & management module

- User entity mapped to PostgreSQL users table (UUID PK)
- PasswordResetToken entity mapped to password_reset_tokens table
- UserRepository & PasswordResetTokenRepository
- DTOs: SignUpRequest, SignInRequest, UpdateProfileRequest,
        ChangePasswordRequest, ForgotPasswordRequest, ResetPasswordRequest
- AuthService with signup, signin, getCurrentUser, updateProfile,
  changePassword, forgotPassword, resetPassword, deleteAccount
- JwtService using jjwt 0.12.6 (token generation + validation)
- JwtAuthenticationFilter (stateless JWT filter)
- SecurityConfig (stateless, JWT-protected endpoints)
- AuthController at /auth with 9 endpoints
- BCrypt password hashing
- application.properties using environment variables for prod"

echo "🔗 Setting remote origin..."
git remote remove origin 2>/dev/null
git remote add origin https://github.com/$GITHUB_ORG/$REPO_NAME.git

echo "🌿 Creating and switching to feature branch..."
git checkout -b $BRANCH_NAME

echo "🚀 Pushing to GitHub..."
git push -u origin $BRANCH_NAME

echo ""
echo "✅ Done! Branch '$BRANCH_NAME' pushed to github.com/$GITHUB_ORG/$REPO_NAME"
echo ""
echo "👉 Next: Go to GitHub and open a Pull Request from '$BRANCH_NAME' → 'main'"

