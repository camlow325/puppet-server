package puppetlabs.jackson.unencoded.impl;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.UTF8JsonGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JackedSonGenerator extends UTF8JsonGenerator {
    private final static byte BYTE_QUOTE = (byte) '"';
    protected final static int BYTE_BACKSLASH = (byte) '\\';

    public JackedSonGenerator(IOContext ctxt, int features, ObjectCodec codec, OutputStream out) {
        super(ctxt, features, codec, out);
    }

    public JackedSonGenerator(IOContext ctxt, int features, ObjectCodec codec, OutputStream out, byte[] outputBuffer, int outputOffset, boolean bufferRecyclable) {
        super(ctxt, features, codec, out, outputBuffer, outputOffset, bufferRecyclable);
        throw new IllegalStateException("Not supported!");
    }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException, JsonGenerationException {
        throw new IllegalStateException("Not supported!");
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException, JsonGenerationException {
        _verifyValueWrite(WRITE_BINARY);
        // Starting quotes
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;


        byte[] encodingBuffer = _ioContext.allocBase64Buffer();
        int bytes;

        try {
            bytes = _writeUnencodedBinary(data, encodingBuffer);
        } finally {
            _ioContext.releaseBase64Buffer(encodingBuffer);
        }

        // and closing quotes
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
        return dataLength;
    }

    private int _writeUnencodedBinary(InputStream data, byte[] readBuffer) throws IOException {

        int inputPtr = readBuffer.length;
        int inputEnd = 0;
        int bytesRead = 0;

        boolean isPrevCharBackslash = false;
        byte b;

        while (true) {
            if (inputPtr >= readBuffer.length) { // need to load more
                inputPtr = 0;
                inputEnd = data.read(readBuffer, 0, readBuffer.length);
                bytesRead += inputEnd;
            }
            while (inputPtr < inputEnd) {
                if (_outputTail >= _outputEnd) {
                    _flushBuffer();
                }
                b = readBuffer[inputPtr];
                if ((!isPrevCharBackslash) && (b == BYTE_QUOTE)) {
                    throw new IllegalStateException("Derp!  You must escape any quote characters in your stream before it can be serialized by JackedSonGenerator.");
                }
                _outputBuffer[_outputTail++] = b;
                inputPtr++;

                if (b == BYTE_BACKSLASH) {
                    isPrevCharBackslash = true;
                } else {
                    isPrevCharBackslash = false;
                }
            }
            if (inputEnd < readBuffer.length) {
                break;
            }
        }

        return bytesRead;
    }

}
