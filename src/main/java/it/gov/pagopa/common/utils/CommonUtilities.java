package it.gov.pagopa.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CommonUtilities {

    private CommonUtilities(){}

    /** To convert cents into euro */
    public static BigDecimal centsToEuro(Long cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_DOWN);
    }

}
