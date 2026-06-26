package com.wiblog.transview.core.common;

import com.wiblog.transview.core.bean.TransViewProperties;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StrategyTypeEnumTest {

    @After
    public void restoreCadConvertType() {
        TransViewProperties.View.Cad.setConvertType(CadConvertType.SVG);
    }

    @Test
    public void getMediaTypeUsesCurrentCadConvertType() {
        TransViewProperties.View.Cad.setConvertType(CadConvertType.PDF);
        assertThat(StrategyTypeEnum.getMediaType("dwg")).isEqualTo(Constant.MediaType.PDF_VALUE);

        TransViewProperties.View.Cad.setConvertType(CadConvertType.SVG);
        assertThat(StrategyTypeEnum.getMediaType("dwg")).isEqualTo(Constant.MediaType.IMAGE_SVG_VALUE);
    }
}
