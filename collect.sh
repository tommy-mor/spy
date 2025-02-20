#!/bin/bash

# Print directory tree
tree .

echo -e "\nFile contents:\n"

# Find and sort all files, excluding hidden directories and prompt.txt
find . -type f -not -path "*/\.*" -not -name "prompt.txt" | sort | while IFS= read -r file; do
    if [ -r "$file" ]; then
        echo -e "\n=== $file ===\n"
        cat "$file" || echo "Error reading $file"
    else
        echo "Cannot read $file"
    fi
done