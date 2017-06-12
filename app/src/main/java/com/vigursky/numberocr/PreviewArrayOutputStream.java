package com.vigursky.numberocr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by vigursky on 18.01.2017.
 */

public class PreviewArrayOutputStream extends ByteArrayOutputStream {

    public byte[] getBytes(){
        return buf;
    }

}
