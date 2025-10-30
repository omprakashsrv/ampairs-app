#!/bin/bash

echo "🔍 Checking Angular project compilation..."
echo "==========================================="

# Navigate to project directory
cd "$(dirname "$0")/.."

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

echo ""
echo "🔨 Building project..."
echo "====================="
npm run build

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo "=================="
    echo "The project compiles without errors."
    echo ""
    echo "📊 Build output:"
    ls -la dist/ampairs-web/
else
    echo ""
    echo "❌ Build failed!"
    echo "==============="
    echo "Please check the errors above and fix them."
    exit 1
fi

echo ""
echo "🧪 Running type check..."
echo "========================"
npx tsc --noEmit

if [ $? -eq 0 ]; then
    echo "✅ Type check passed!"
else
    echo "❌ Type check failed!"
    exit 1
fi

echo ""
echo "🎉 All checks passed!"
echo "===================="
echo "Your Angular project is ready to run!"
echo ""
echo "To start development server:"
echo "npm start"