package custis.easyabac.core.init;

import custis.easyabac.core.model.attribute.AttributeValue;
import custis.easyabac.pdp.AuthResponse;

import java.util.List;

public interface PdpHandler {
    AuthResponse evaluate(List<AttributeValue> attributeValues);
}
