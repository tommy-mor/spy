ORIGINAL_DIR=$(pwd)
cd "$(dirname "$0")"
[ -f prompt.txt ] && rm prompt.txt
./collect.sh > prompt.txt && open prompt.txt
cd "$ORIGINAL_DIR"