<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xpath-default-namespace="" xmlns="statistics:2" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="2.0">
  <xsl:template match="data">
    <statistics xsi:schemaLocation="statistics:2 file://statistics-v2.0.xsd">
      <xsl:apply-templates />
    </statistics>
  </xsl:template>
  <xsl:template match="event">
    <statistic>
      <time>
        <xsl:value-of select="./time" />
      </time>
      <value>
        <xsl:value-of select="./value" />
      </value>
      <tags>
        <tag>
          <xsl:attribute name="name">user</xsl:attribute>
          <xsl:attribute name="value">
            <xsl:value-of select="./user" />
          </xsl:attribute>
        </tag>
        <tag>
          <xsl:attribute name="name">colour</xsl:attribute>
          <xsl:attribute name="value">
            <xsl:value-of select="./colour" />
          </xsl:attribute>
        </tag>
        <tag>
          <xsl:attribute name="name">state</xsl:attribute>
          <xsl:attribute name="value">
            <xsl:value-of select="./state" />
          </xsl:attribute>
        </tag>
      </tags>
    </statistic>
  </xsl:template>
</xsl:stylesheet>
