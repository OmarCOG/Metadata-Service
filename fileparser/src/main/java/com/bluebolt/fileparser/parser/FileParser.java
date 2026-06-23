package com.bluebolt.fileparser.parser;

import com.bluebolt.fileparser.model.ParsedFile;
import java.io.InputStream;

public interface FileParser {
    ParsedFile parse(InputStream inputStream, String fileName);
}
