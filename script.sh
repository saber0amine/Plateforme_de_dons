#!/bin/bash

# Define source directories
SRC_JAVA="src/main/java/com/dev/plateforme_de_dons"
SRC_RESOURCES="src/main/resources"
DEST="myfiles"

# Create the destination directory if it doesn't exist
mkdir -p "$DEST"

# Function to copy and rename files based on parent directory
copy_and_rename() {
    local src_dir=$1
    local dest_dir=$2

    find "$src_dir" -type f | while read -r file; do
        parent_dir=$(basename "$(dirname "$file")")
        filename=$(basename "$file")
        cp "$file" "$dest_dir/${parent_dir}_${filename}"
    done
}

# Copy and rename Java files
copy_and_rename "$SRC_JAVA/config" "$DEST"
copy_and_rename "$SRC_JAVA/controller" "$DEST"
copy_and_rename "$SRC_JAVA/dto" "$DEST"
copy_and_rename "$SRC_JAVA/model" "$DEST"
copy_and_rename "$SRC_JAVA/repository" "$DEST"
copy_and_rename "$SRC_JAVA/service" "$DEST"
cp "$SRC_JAVA/PlateformeDeDonsApplication.java" "$DEST/PlateformeDeDonsApplication.java"

# Copy and rename resource files
copy_and_rename "$SRC_RESOURCES/static" "$DEST"
copy_and_rename "$SRC_RESOURCES/templates" "$DEST"

echo "All files have been copied and renamed to the '$DEST' directory."
