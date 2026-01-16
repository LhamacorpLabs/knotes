#!/bin/bash

# Script to update desktop app with latest frontend changes
# Usage: ./update-frontend.sh

echo "🔄 Updating desktop app with latest frontend changes..."

# Check if we're in the right directory
if [ ! -f "package.json" ] || [ ! -f "../src/main/resources/static/index.html" ]; then
    echo "❌ Error: Please run this script from the desktop-app directory"
    exit 1
fi

# Copy frontend files
echo "📁 Copying frontend files..."
cp -r ../src/main/resources/static/* . 2>/dev/null || {
    echo "❌ Error: Could not copy frontend files"
    exit 1
}

# Check if files were copied successfully
if [ -f "index.html" ] && [ -f "home.html" ] && [ -d "js" ] && [ -d "css" ]; then
    echo "✅ Frontend files updated successfully"
    echo "🏗️  You can now run 'npm run build-linux' to create a new build"
    echo "🚀 Or run 'npm start' to test the changes"
else
    echo "❌ Error: Some frontend files may not have been copied correctly"
    exit 1
fi

echo "✨ Update complete!"