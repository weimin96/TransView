package com.wiblog.transview.core.common;

import com.wiblog.transview.core.bean.TransViewProperties;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StrategyTypeEnumTest {

    @After
    public void restoreConvertType() {
        TransViewProperties.View.Cad.setConvertType(CadConvertType.SVG);
        TransViewProperties.View.Word.setConvertType(WordConvertType.PDF);
    }

    @Test
    public void getMediaTypeUsesCurrentCadConvertType() {
        TransViewProperties.View.Cad.setConvertType(CadConvertType.PDF);
        assertThat(StrategyTypeEnum.getMediaType("dwg")).isEqualTo(Constant.MediaType.PDF_VALUE);

        TransViewProperties.View.Cad.setConvertType(CadConvertType.SVG);
        assertThat(StrategyTypeEnum.getMediaType("dwg")).isEqualTo(Constant.MediaType.IMAGE_SVG_VALUE);
    }

    @Test
    public void getMediaTypeUsesCurrentWordConvertType() {
        TransViewProperties.View.Word.setConvertType(WordConvertType.PDF);
        assertThat(StrategyTypeEnum.getMediaType("docx")).isEqualTo(Constant.MediaType.PDF_VALUE);
        assertThat(StrategyTypeEnum.getMediaType("wps")).isEqualTo(Constant.MediaType.PDF_VALUE);

        TransViewProperties.View.Word.setConvertType(WordConvertType.SVG);
        assertThat(StrategyTypeEnum.getMediaType("docx")).isEqualTo(Constant.MediaType.IMAGE_SVG_VALUE);
        assertThat(StrategyTypeEnum.getMediaType("wps")).isEqualTo(Constant.MediaType.IMAGE_SVG_VALUE);
    }

    @Test
    public void getMediaTypeUsesPdfForOfd() {
        TransViewProperties.View.Word.setConvertType(WordConvertType.SVG);

        assertThat(StrategyTypeEnum.getMediaType("ofd")).isEqualTo(Constant.MediaType.PDF_VALUE);
    }
}
