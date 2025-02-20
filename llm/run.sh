# Get the directory containing the script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ORIGINAL_DIR=$(pwd)

# If we're in the parent directory, cd into llm
if [[ "$(basename "$SCRIPT_DIR")" != "llm" ]]; then
    cd "$SCRIPT_DIR/llm"
else
    cd "$SCRIPT_DIR"
fi

[ -f prompt.txt ] && rm prompt.txt
./collect.sh > prompt.txt && open prompt.txt
cd "$ORIGINAL_DIR"