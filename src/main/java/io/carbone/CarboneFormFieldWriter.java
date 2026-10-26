package io.carbone;

import feign.codec.EncodeException;
import feign.form.ContentType;
import feign.form.FormEncoder;
import feign.form.MultipartFormContentProcessor;
import feign.form.multipart.AbstractWriter;
import feign.form.multipart.Output;

/**
 * Writes multipart text fields without a Content-Type header. The Carbone API ignores
 * text fields that have one, and feign-form's default writer always adds "text/plain".
 */
class CarboneFormFieldWriter extends AbstractWriter {

    static FormEncoder formEncoder() {
        FormEncoder encoder = new FormEncoder();
        ((MultipartFormContentProcessor) encoder.getContentProcessor(ContentType.MULTIPART))
            .addFirstWriter(new CarboneFormFieldWriter());
        return encoder;
    }

    @Override
    public boolean isApplicable(Object value) {
        return value instanceof String;
    }

    @Override
    protected void write(Output output, String key, Object value) throws EncodeException {
        output.write("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n");
        output.write(value.toString());
    }
}
