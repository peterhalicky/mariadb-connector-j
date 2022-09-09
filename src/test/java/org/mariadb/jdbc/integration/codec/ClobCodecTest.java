// SPDX-License-Identifier: LGPL-2.1-or-later
// Copyright (c) 2012-2014 Monty Program Ab
// Copyright (c) 2015-2021 MariaDB Corporation Ab

package org.mariadb.jdbc.integration.codec;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbClob;
import org.mariadb.jdbc.Statement;
import org.mariadb.jdbc.integration.Common;

public class ClobCodecTest extends CommonCodecTest {
  @AfterAll
  public static void drop() throws SQLException {
    Statement stmt = sharedConn.createStatement();
    stmt.execute("DROP TABLE IF EXISTS ClobCodec");
    stmt.execute("DROP TABLE IF EXISTS ClobParamCodec");
  }

  @BeforeAll
  public static void beforeAll2() throws SQLException {
    drop();
    Statement stmt = sharedConn.createStatement();
    stmt.execute(
        "CREATE TABLE ClobCodec (t1 TINYTEXT, t2 TEXT, t3 MEDIUMTEXT, t4 LONGTEXT) CHARACTER "
            + "SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    stmt.execute(
        "INSERT INTO ClobCodec VALUES ('0', '1', 'some🌟', null), ('2011-01-01', '2010-12-31 23:59:59.152',"
            + " '23:54:51.840010', null)");
    stmt.execute(
        "CREATE TABLE ClobParamCodec(id int not null primary key auto_increment, t1 TEXT) "
            + "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

    stmt.execute("FLUSH TABLES");
  }

  private ResultSet get() throws SQLException {
    Statement stmt = sharedConn.createStatement();
    stmt.execute("START TRANSACTION"); // if MAXSCALE ensure using WRITER
    ResultSet rs =
        stmt.executeQuery(
            "select t1 as t1alias, t2 as t2alias, t3 as t3alias, t4 as t4alias from ClobCodec");
    assertTrue(rs.next());
    sharedConn.commit();
    return rs;
  }

  private ResultSet getPrepare(Connection con) throws SQLException {
    java.sql.Statement stmt = con.createStatement();
    stmt.execute("START TRANSACTION"); // if MAXSCALE ensure using WRITER
    PreparedStatement preparedStatement =
        con.prepareStatement(
            "select t1 as t1alias, t2 as t2alias, t3 as t3alias, t4 as t4alias from ClobCodec"
                + " WHERE 1 > ?");
    preparedStatement.closeOnCompletion();
    preparedStatement.setInt(1, 0);
    ResultSet rs = preparedStatement.executeQuery();
    assertTrue(rs.next());
    con.commit();
    return rs;
  }

  @Test
  public void getObject() throws SQLException {
    getObject(get());
  }

  @Test
  public void getObjectPrepare() throws SQLException {
    getObject(getPrepare(sharedConn));
    getObject(getPrepare(sharedConnBinary));
  }

  public void getObject(ResultSet rs) throws SQLException {
    assertEquals(new MariaDbClob("0".getBytes()), rs.getObject(1, Clob.class));
    assertFalse(rs.wasNull());
    assertEquals(new MariaDbClob("1".getBytes()), rs.getObject(2, Clob.class));
    assertEquals(new MariaDbClob("1".getBytes()), rs.getObject("t2alias", Clob.class));
    assertFalse(rs.wasNull());
    assertEquals(
        new MariaDbClob("some🌟".getBytes(StandardCharsets.UTF_8)), rs.getObject(3, Clob.class));
    assertFalse(rs.wasNull());
    assertNull(rs.getObject(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getObjectType() throws Exception {
    getObjectType(get());
  }

  @Test
  public void getObjectTypePrepare() throws Exception {
    getObjectType(getPrepare(sharedConn));
    getObjectType(getPrepare(sharedConnBinary));
  }

  public void getObjectType(ResultSet rs) throws Exception {
    testObject(rs, Integer.class, 0);
    testObject(rs, String.class, "0");
    testObject(rs, Byte.class, Byte.valueOf("0"));
    testObject(rs, Long.class, 0L);
    testObject(rs, Short.class, (short) 0);
    testObject(rs, BigDecimal.class, BigDecimal.valueOf(0));
    testObject(rs, BigInteger.class, BigInteger.valueOf(0));
    testObject(rs, Double.class, (double) 0);
    testObject(rs, Float.class, (float) 0);
    testObject(rs, Byte.class, (byte) 0);
    testArrObject(rs, new byte[] {(byte) '0'});
    testObject(rs, Boolean.class, Boolean.FALSE);
    testObject(rs, Clob.class, new MariaDbClob("0".getBytes()));
    testObject(rs, NClob.class, new MariaDbClob("0".getBytes()));
    testObject(rs, InputStream.class, new MariaDbClob("0".getBytes()).getBinaryStream());
    testObject(rs, Reader.class, new StringReader("0"));
    rs.next();
    testObject(rs, LocalDate.class, LocalDate.parse("2011-01-01"));
    testObject(rs, LocalDateTime.class, LocalDateTime.parse("2011-01-01T00:00"));
    testObject(rs, LocalTime.class, LocalTime.parse("23:54:51.840010"), 3);
    Time t = Time.valueOf("23:54:51");
    testObject(rs, Time.class, new Time(t.getTime() + 840), 3);
    testObject(rs, Date.class, Date.valueOf("2011-01-01"));
    Timestamp tt = Timestamp.valueOf("2010-12-31 23:59:59");
    testObject(rs, Timestamp.class, new Timestamp(tt.getTime() + 152), 2);
    testObject(
        rs,
        ZonedDateTime.class,
        LocalDateTime.parse("2011-01-01T00:00").atZone(ZoneId.systemDefault()));
    testErrObject(rs, BigInteger.class);
    testErrObject(rs, OffsetDateTime.class);
    testErrObject(rs, OffsetTime.class);
    testObject(rs, java.util.Date.class, Date.valueOf("2010-12-31"), 2);
  }

  @Test
  public void getString() throws SQLException {
    getString(get());
  }

  @Test
  public void getStringPrepare() throws SQLException {
    getString(getPrepare(sharedConn));
    getString(getPrepare(sharedConnBinary));
  }

  public void getString(ResultSet rs) throws SQLException {
    assertEquals("0", rs.getString(1));
    assertFalse(rs.wasNull());
    assertEquals("1", rs.getString(2));
    assertEquals("1", rs.getString("t2alias"));
    assertFalse(rs.wasNull());
    assertEquals("some🌟", rs.getString(3));
    assertFalse(rs.wasNull());
    assertNull(rs.getString(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getNString() throws SQLException {
    getNString(get());
  }

  @Test
  public void getNStringPrepare() throws SQLException {
    getNString(getPrepare(sharedConn));
    getNString(getPrepare(sharedConnBinary));
  }

  public void getNString(ResultSet rs) throws SQLException {
    assertEquals("0", rs.getNString(1));
    assertFalse(rs.wasNull());
    assertEquals("1", rs.getNString(2));
    assertEquals("1", rs.getNString("t2alias"));
    assertFalse(rs.wasNull());
    assertEquals("some🌟", rs.getNString(3));
    assertFalse(rs.wasNull());
    assertNull(rs.getNString(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getBoolean() throws SQLException {
    getBoolean(get());
  }

  @Test
  public void getBooleanPrepare() throws SQLException {
    getBoolean(getPrepare(sharedConn));
    getBoolean(getPrepare(sharedConnBinary));
  }

  public void getBoolean(ResultSet rs) throws SQLException {
    assertFalse(rs.getBoolean(1));
    assertFalse(rs.wasNull());
    assertTrue(rs.getBoolean(2));
    assertTrue(rs.getBoolean("t2alias"));
    assertFalse(rs.wasNull());
    assertTrue(rs.getBoolean(3));
    assertFalse(rs.wasNull());
    assertFalse(rs.getBoolean(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getByte() throws SQLException {
    getByte(get());
  }

  @Test
  public void getBytePrepare() throws SQLException {
    getByte(getPrepare(sharedConn));
    getByte(getPrepare(sharedConnBinary));
  }

  public void getByte(ResultSet rs) throws SQLException {
    assertEquals((byte) 0, rs.getByte(1));
    assertFalse(rs.wasNull());
    assertEquals((byte) 1, rs.getByte(2));
    assertEquals((byte) 1, rs.getByte("t2alias"));
    assertFalse(rs.wasNull());
    Common.assertThrowsContains(
        SQLDataException.class,
        () -> rs.getByte(3),
        "value 'some\uD83C\uDF1F' (BLOB) cannot be decoded as Byte");
    assertFalse(rs.wasNull());
    assertEquals((byte) 0, rs.getByte(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getShort() throws SQLException {
    getShort(get());
  }

  @Test
  public void getShortPrepare() throws SQLException {
    getShort(getPrepare(sharedConn));
    getShort(getPrepare(sharedConnBinary));
  }

  public void getShort(ResultSet rs) throws SQLException {
    assertEquals(0, rs.getShort(1));
    assertFalse(rs.wasNull());
    assertEquals(1, rs.getShort(2));
    assertEquals(1, rs.getShort("t2alias"));
    assertFalse(rs.wasNull());
    Common.assertThrowsContains(
        SQLDataException.class, () -> rs.getShort(3), "value 'some🌟' cannot be decoded as Short");
    assertFalse(rs.wasNull());
    assertEquals(0, rs.getShort(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getInt() throws SQLException {
    getInt(get());
  }

  @Test
  public void getIntPrepare() throws SQLException {
    getInt(getPrepare(sharedConn));
    getInt(getPrepare(sharedConnBinary));
  }

  public void getInt(ResultSet rs) throws SQLException {
    assertEquals(0, rs.getInt(1));
    assertFalse(rs.wasNull());
    assertEquals(1, rs.getInt(2));
    assertEquals(1, rs.getInt("t2alias"));
    assertFalse(rs.wasNull());
    Common.assertThrowsContains(
        SQLDataException.class, () -> rs.getInt(3), "value 'some🌟' cannot be decoded as Integer");
    assertFalse(rs.wasNull());
    assertEquals(0, rs.getInt(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getLong() throws SQLException {
    getLong(get());
  }

  @Test
  public void getLongPrepare() throws SQLException {
    getLong(getPrepare(sharedConn));
    getLong(getPrepare(sharedConnBinary));
  }

  public void getLong(ResultSet rs) throws SQLException {
    assertEquals(0L, rs.getLong(1));
    assertFalse(rs.wasNull());
    assertEquals(1L, rs.getLong(2));
    assertEquals(1L, rs.getLong("t2alias"));
    assertFalse(rs.wasNull());
    Common.assertThrowsContains(
        SQLDataException.class,
        () -> rs.getLong(3),
        "value 'some\uD83C\uDF1F' cannot be decoded as Long");
    assertFalse(rs.wasNull());
    assertEquals(0L, rs.getLong(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getFloat() throws SQLException {
    getFloat(get());
  }

  @Test
  public void getFloatPrepare() throws SQLException {
    getFloat(getPrepare(sharedConn));
    getFloat(getPrepare(sharedConnBinary));
  }

  public void getFloat(ResultSet rs) throws SQLException {
    assertEquals(0F, rs.getFloat(1));
    assertFalse(rs.wasNull());
    assertEquals(1F, rs.getFloat(2));
    assertEquals(1F, rs.getFloat("t2alias"));
    assertFalse(rs.wasNull());
    Common.assertThrowsContains(
        SQLDataException.class, () -> rs.getFloat(3), "value 'some🌟' cannot be decoded as Float");
    assertFalse(rs.wasNull());
    assertEquals(0F, rs.getFloat(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getDouble() throws SQLException {
    getDouble(get());
  }

  @Test
  public void getDoublePrepare() throws SQLException {
    getDouble(getPrepare(sharedConn));
    getDouble(getPrepare(sharedConnBinary));
  }

  public void getDouble(ResultSet rs) throws SQLException {
    assertEquals(0D, rs.getDouble(1));
    assertFalse(rs.wasNull());
    assertEquals(1D, rs.getDouble(2));
    assertEquals(1D, rs.getDouble("t2alias"));
    assertFalse(rs.wasNull());
    Common.assertThrowsContains(
        SQLDataException.class,
        () -> rs.getDouble(3),
        "value 'some🌟' cannot be decoded as Double");
    assertFalse(rs.wasNull());
    assertEquals(0D, rs.getDouble(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getBigDecimal() throws SQLException {
    getBigDecimal(get());
  }

  @Test
  public void getBigDecimalPrepare() throws SQLException {
    getBigDecimal(getPrepare(sharedConn));
    getBigDecimal(getPrepare(sharedConnBinary));
  }

  public void getBigDecimal(ResultSet rs) throws SQLException {
    assertEquals(BigDecimal.ZERO, rs.getBigDecimal(1));
    assertFalse(rs.wasNull());
    assertEquals(BigDecimal.ONE, rs.getBigDecimal(2));
    assertEquals(BigDecimal.ONE, rs.getBigDecimal("t2alias"));
    assertFalse(rs.wasNull());
    Common.assertThrowsContains(
        SQLDataException.class,
        () -> rs.getBigDecimal(3),
        "value 'some🌟' cannot be decoded as BigDecimal");
    assertFalse(rs.wasNull());
    assertNull(rs.getBigDecimal(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getDate() throws SQLException {
    getDate(get());
  }

  @Test
  public void getDatePrepare() throws SQLException {
    getDate(getPrepare(sharedConn));
    getDate(getPrepare(sharedConnBinary));
  }

  public void getDate(ResultSet rs) throws SQLException {
    Common.assertThrowsContains(
        SQLDataException.class, () -> rs.getDate(1), "value '0' (BLOB) cannot be decoded as Date");
    rs.next();
    assertEquals("2011-01-01", rs.getDate(1).toString());
    assertFalse(rs.wasNull());
    assertEquals("2010-12-31", rs.getDate(2).toString());
    assertFalse(rs.wasNull());
    assertEquals("2010-12-31", rs.getDate("t2alias").toString());
    assertFalse(rs.wasNull());
    Common.assertThrowsContains(
        SQLDataException.class,
        () -> rs.getDate(3),
        "value '23:54:51.840010' (BLOB) cannot be decoded as Date");
    assertFalse(rs.wasNull());
    assertNull(rs.getDate(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getTime() throws SQLException {
    getTime(get());
  }

  @Test
  public void getTimePrepare() throws SQLException {
    getTime(getPrepare(sharedConn));
    getTime(getPrepare(sharedConnBinary));
  }

  public void getTime(ResultSet rs) throws SQLException {
    rs.next();
    Common.assertThrowsContains(
        SQLDataException.class,
        () -> rs.getTime(1),
        "BLOB value '2011-01-01' cannot be decoded as Time");
    assertFalse(rs.wasNull());
    Common.assertThrowsContains(
        SQLDataException.class,
        () -> rs.getTime(2),
        "BLOB value '2010-12-31 23:59:59.152' cannot be decoded as Time");
    assertEquals(Time.valueOf("23:54:51").getTime() + 840, rs.getTime(3).getTime());
    assertEquals(Time.valueOf("23:54:51").getTime() + 840, rs.getTime("t3alias").getTime());
    Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    assertEquals(86091840, rs.getTime(3, utc).getTime());
    assertEquals(86091840, rs.getTime("t3alias", utc).getTime());

    assertFalse(rs.wasNull());
  }

  @Test
  public void getTimestamp() throws SQLException {
    getTimestamp(get());
  }

  @Test
  public void getTimestampPrepare() throws SQLException {
    getTimestamp(getPrepare(sharedConn));
    getTimestamp(getPrepare(sharedConnBinary));
  }

  public void getTimestamp(ResultSet rs) throws SQLException {
    rs.next();

    assertEquals(Timestamp.valueOf("2011-01-01 00:00:00").getTime(), rs.getTimestamp(1).getTime());
    assertEquals(
        Timestamp.valueOf("2011-01-01 00:00:00").getTime() + TimeZone.getDefault().getDSTSavings(),
        rs.getTimestamp(1, Calendar.getInstance(TimeZone.getTimeZone("UTC"))).getTime());
    assertEquals(
        Timestamp.valueOf("2011-01-01 00:00:00").getTime(), rs.getTimestamp("t1alias").getTime());
    assertEquals(
        Timestamp.valueOf("2011-01-01 00:00:00").getTime() + TimeZone.getDefault().getDSTSavings(),
        rs.getTimestamp("t1alias", Calendar.getInstance(TimeZone.getTimeZone("UTC"))).getTime());
    assertFalse(rs.wasNull());

    assertEquals(
        Timestamp.valueOf("2010-12-31 23:59:59").getTime() + 152, rs.getTimestamp(2).getTime());
    assertEquals(
        Timestamp.valueOf("2010-12-31 23:59:59").getTime()
            + 152
            + TimeZone.getDefault().getDSTSavings(),
        rs.getTimestamp(2, Calendar.getInstance(TimeZone.getTimeZone("UTC"))).getTime());
    assertEquals(
        Timestamp.valueOf("2010-12-31 23:59:59").getTime() + 152,
        rs.getTimestamp("t2alias").getTime());
    assertEquals(
        Timestamp.valueOf("2010-12-31 23:59:59").getTime()
            + 152
            + TimeZone.getDefault().getDSTSavings(),
        rs.getTimestamp("t2alias", Calendar.getInstance(TimeZone.getTimeZone("UTC"))).getTime());
    assertFalse(rs.wasNull());

    //
    //    ('2011-01-01', '2010-12-31 23:59:59.152',"
    //            + " '23:54:51.840010', null)
  }

  @Test
  public void getOffsetDateTime() throws SQLException {
    getOffsetDateTime(get());
  }

  @Test
  public void getOffsetDateTimePrepare() throws SQLException {
    getOffsetDateTime(getPrepare(sharedConn));
    getOffsetDateTime(getPrepare(sharedConnBinary));
  }

  public void getOffsetDateTime(ResultSet rs) throws SQLException {
    Common.assertThrowsContains(
        SQLException.class,
        () -> rs.getObject(1, OffsetDateTime.class),
        "cannot be decoded as OffsetDateTime");
  }

  @Test
  public void getAsciiStream() throws Exception {
    getAsciiStream(get());
  }

  @Test
  public void getAsciiStreamPrepare() throws Exception {
    getAsciiStream(getPrepare(sharedConn));
    getAsciiStream(getPrepare(sharedConnBinary));
  }

  public void getAsciiStream(ResultSet rs) throws Exception {
    assertStreamEquals(new ByteArrayInputStream("0".getBytes()), rs.getAsciiStream(1));
    assertFalse(rs.wasNull());
    assertStreamEquals(new ByteArrayInputStream("1".getBytes()), rs.getAsciiStream(2));
    assertStreamEquals(new ByteArrayInputStream("1".getBytes()), rs.getAsciiStream("t2alias"));
    assertFalse(rs.wasNull());
    assertStreamEquals(
        new ByteArrayInputStream("some🌟".getBytes(StandardCharsets.UTF_8)), rs.getAsciiStream(3));
    assertFalse(rs.wasNull());
    assertNull(rs.getAsciiStream(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getUnicodeStream() throws Exception {
    getUnicodeStream(get());
  }

  @Test
  public void getUnicodeStreamPrepare() throws Exception {
    getUnicodeStream(getPrepare(sharedConn));
    getUnicodeStream(getPrepare(sharedConnBinary));
  }

  @SuppressWarnings("deprecation")
  public void getUnicodeStream(ResultSet rs) throws Exception {
    assertStreamEquals(new ByteArrayInputStream("0".getBytes()), rs.getUnicodeStream(1));
    assertFalse(rs.wasNull());
    assertStreamEquals(new ByteArrayInputStream("1".getBytes()), rs.getUnicodeStream(2));
    assertStreamEquals(new ByteArrayInputStream("1".getBytes()), rs.getUnicodeStream("t2alias"));
    assertFalse(rs.wasNull());
    assertStreamEquals(
        new ByteArrayInputStream("some🌟".getBytes(StandardCharsets.UTF_8)),
        rs.getUnicodeStream(3));
    assertFalse(rs.wasNull());
    assertNull(rs.getUnicodeStream(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getBinaryStream() throws Exception {
    getBinaryStream(get());
  }

  @Test
  public void getBinaryStreamPrepare() throws Exception {
    getBinaryStream(getPrepare(sharedConn));
    getBinaryStream(getPrepare(sharedConnBinary));
  }

  public void getBinaryStream(ResultSet rs) throws Exception {
    assertStreamEquals(new ByteArrayInputStream("0".getBytes()), rs.getBinaryStream(1));
    assertFalse(rs.wasNull());
    assertStreamEquals(new ByteArrayInputStream("1".getBytes()), rs.getBinaryStream(2));
    assertStreamEquals(new ByteArrayInputStream("1".getBytes()), rs.getBinaryStream("t2alias"));
    assertFalse(rs.wasNull());
    assertStreamEquals(
        new ByteArrayInputStream("some🌟".getBytes(StandardCharsets.UTF_8)), rs.getBinaryStream(3));
    assertFalse(rs.wasNull());
    assertNull(rs.getBinaryStream(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getBytes() throws SQLException {
    getBytes(get());
  }

  @Test
  public void getBytesPrepare() throws SQLException {
    getBytes(getPrepare(sharedConn));
    getBytes(getPrepare(sharedConnBinary));
  }

  public void getBytes(ResultSet rs) throws SQLException {
    assertArrayEquals("0".getBytes(), rs.getBytes(1));
    assertFalse(rs.wasNull());
    assertArrayEquals("1".getBytes(), rs.getBytes(2));
    assertArrayEquals("1".getBytes(), rs.getBytes("t2alias"));
    assertFalse(rs.wasNull());
    assertArrayEquals("some🌟".getBytes(StandardCharsets.UTF_8), rs.getBytes(3));
    assertFalse(rs.wasNull());
    assertNull(rs.getBytes(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getCharacterStream() throws Exception {
    getCharacterStream(get());
  }

  @Test
  public void getCharacterStreamPrepare() throws Exception {
    getCharacterStream(getPrepare(sharedConn));
    getCharacterStream(getPrepare(sharedConnBinary));
  }

  public void getCharacterStream(ResultSet rs) throws Exception {
    assertReaderEquals(new StringReader("0"), rs.getCharacterStream(1));
    assertFalse(rs.wasNull());
    assertReaderEquals(new StringReader("1"), rs.getCharacterStream(2));
    assertReaderEquals(new StringReader("1"), rs.getCharacterStream("t2alias"));
    assertFalse(rs.wasNull());
    assertReaderEquals(new StringReader("some🌟"), rs.getCharacterStream(3));
    assertFalse(rs.wasNull());
    assertNull(rs.getCharacterStream(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getNCharacterStream() throws Exception {
    getNCharacterStream(get());
  }

  @Test
  public void getNCharacterStreamPrepare() throws Exception {
    getNCharacterStream(getPrepare(sharedConn));
    getNCharacterStream(getPrepare(sharedConnBinary));
  }

  public void getNCharacterStream(ResultSet rs) throws Exception {
    assertReaderEquals(new StringReader("0"), rs.getNCharacterStream(1));
    assertFalse(rs.wasNull());
    assertReaderEquals(new StringReader("1"), rs.getNCharacterStream(2));
    assertReaderEquals(new StringReader("1"), rs.getNCharacterStream("t2alias"));
    assertFalse(rs.wasNull());
    assertReaderEquals(new StringReader("some🌟"), rs.getNCharacterStream(3));
    assertFalse(rs.wasNull());
    assertNull(rs.getNCharacterStream(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getBlob() throws Exception {
    getBlob(get());
  }

  @Test
  public void getBlobPrepare() throws Exception {
    getBlob(getPrepare(sharedConn));
    getBlob(getPrepare(sharedConnBinary));
  }

  public void getBlob(ResultSet rs) {
    Common.assertThrowsContains(
        SQLDataException.class,
        () -> rs.getBlob(1),
        "Data type BLOB (not binary) cannot be decoded as Blob");
  }

  @Test
  public void getClob() throws Exception {
    getClob(get());
  }

  @Test
  public void getClobPrepare() throws Exception {
    getClob(getPrepare(sharedConn));
    getClob(getPrepare(sharedConnBinary));
  }

  public void getClob(ResultSet rs) throws Exception {
    assertStreamEquals(new MariaDbClob("0".getBytes()), rs.getClob(1));
    assertFalse(rs.wasNull());
    assertStreamEquals(new MariaDbClob("1".getBytes()), rs.getClob(2));
    assertStreamEquals(new MariaDbClob("1".getBytes()), rs.getClob("t2alias"));
    assertFalse(rs.wasNull());
    assertStreamEquals(new MariaDbClob("some🌟".getBytes(StandardCharsets.UTF_8)), rs.getClob(3));
    assertFalse(rs.wasNull());
    assertNull(rs.getClob(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getNClob() throws Exception {
    getNClob(get());
  }

  @Test
  public void getNClobPrepare() throws Exception {
    getNClob(getPrepare(sharedConn));
    getNClob(getPrepare(sharedConnBinary));
  }

  public void getNClob(ResultSet rs) throws Exception {
    assertStreamEquals(new MariaDbClob("0".getBytes()), rs.getNClob(1));
    assertFalse(rs.wasNull());
    assertStreamEquals(new MariaDbClob("1".getBytes()), rs.getNClob(2));
    assertStreamEquals(new MariaDbClob("1".getBytes()), rs.getNClob("t2alias"));
    assertFalse(rs.wasNull());
    assertStreamEquals(new MariaDbClob("some🌟".getBytes(StandardCharsets.UTF_8)), rs.getNClob(3));
    assertFalse(rs.wasNull());
    assertNull(rs.getNClob(4));
    assertTrue(rs.wasNull());
  }

  @Test
  public void getMetaData() throws SQLException {
    ResultSet rs = get();
    ResultSetMetaData meta = rs.getMetaData();
    assertEquals("VARCHAR", meta.getColumnTypeName(1));
    assertEquals(sharedConn.getCatalog(), meta.getCatalogName(1));
    assertEquals("java.lang.String", meta.getColumnClassName(1));
    assertEquals("t1alias", meta.getColumnLabel(1));
    assertEquals("t1", meta.getColumnName(1));
    assertEquals(Types.VARCHAR, meta.getColumnType(1));
    assertEquals(4, meta.getColumnCount());
    assertEquals(0, meta.getScale(1));
    assertEquals("", meta.getSchemaName(1));
    if (!isXpand()) {
      assertEquals(255, meta.getColumnDisplaySize(1));
      assertEquals(255, meta.getPrecision(1));
    }
  }

  @Test
  public void sendParam() throws SQLException {
    sendParam(sharedConn);
    sendParam(sharedConnBinary);
    String urlWithHaMode =
        mDefUrl.replaceAll("jdbc:mariadb:", "jdbc:mariadb:sequential:")
            + (mDefUrl.indexOf("?") > 0 ? "&" : "?")
            + "useServerPrepStmts=true";
    try (Connection con = DriverManager.getConnection(urlWithHaMode)) {
      sendParam(con);
    }

    try (Connection con = createCon("transactionReplay=true&useServerPrepStmts=false")) {
      sendParam(con);
    }
    try (Connection con = createCon("transactionReplay=true&useServerPrepStmts=true")) {
      sendParam(con);
    }
  }

  private void sendParam(Connection con) throws SQLException {
    StringBuilder longDataSb = new StringBuilder(20000);

    for (int i = 0; i < 20000; i++) {
      longDataSb.append('0' + i % 16);
    }
    Clob longData = new MariaDbClob(longDataSb.toString().getBytes(StandardCharsets.UTF_8));
    java.sql.Statement stmt = con.createStatement();
    stmt.execute("TRUNCATE TABLE ClobParamCodec");
    stmt.execute("START TRANSACTION"); // if MAXSCALE ensure using WRITER
    try (PreparedStatement prep =
        con.prepareStatement("INSERT INTO ClobParamCodec(t1) VALUES (?)")) {
      prep.setClob(1, new MariaDbClob("e🌟£1".getBytes(StandardCharsets.UTF_8)));
      prep.execute();
      prep.setClob(1, (Clob) null);
      prep.execute();
      prep.setObject(1, "e🌟2");
      prep.execute();
      prep.setObject(1, null);
      prep.execute();
      prep.setObject(1, "e🌟3", Types.VARCHAR);
      prep.execute();
      prep.setObject(1, null, Types.VARCHAR);
      prep.execute();
      prep.setObject(1, null, Types.VARCHAR);
      prep.execute();
      prep.setObject(1, new MariaDbClob("e🌟56".getBytes(StandardCharsets.UTF_8)), Types.CLOB, 4);
      prep.execute();
      prep.setObject(1, longData);

      prep.setClob(1, new MariaDbClob("e🌟1".getBytes(StandardCharsets.UTF_8)));
      prep.addBatch();
      prep.setClob(1, new MariaDbClob("e🌟1".getBytes(StandardCharsets.UTF_8)));
      prep.addBatch();
      prep.setClob(1, longData);
      prep.addBatch();
      prep.setClob(1, (Clob) null);
      prep.addBatch();
      prep.setObject(1, new MariaDbClob("e🌟56".getBytes(StandardCharsets.UTF_8)), Types.CLOB, 4);
      prep.addBatch();
      prep.executeBatch();

      prep.setCharacterStream(1, new StringReader("e🌟789"));
      prep.execute();
      prep.setCharacterStream(1, new StringReader("e🌟890"), 4);
      prep.execute();
      prep.setObject(1, new StringReader(longDataSb.toString()), Types.CLOB);
      prep.execute();
      prep.setObject(1, new StringReader("e🌟568"), Types.CLOB, 4);
      prep.execute();

      prep.setCharacterStream(1, new StringReader("e🌟789"));
      prep.addBatch();
      prep.setCharacterStream(1, new StringReader("e🌟890"), 4);
      prep.addBatch();
      prep.executeBatch();

      prep.setNClob(1, new MariaDbClob("e🌟1".getBytes(StandardCharsets.UTF_8)));
      prep.execute();

      prep.setNClob(1, new MariaDbClob("e🌟145".getBytes(StandardCharsets.UTF_8)));
      prep.execute();

      prep.setNCharacterStream(1, new StringReader("e🌟789"));
      prep.execute();
      prep.setNCharacterStream(1, new StringReader("e🌟890"), 4);
      prep.execute();
      prep.setNCharacterStream(1, new StringReader("e🌟789"));
      prep.execute();
      prep.setNCharacterStream(1, new StringReader("e🌟890"), 4);
      prep.execute();
    }

    ResultSet rs =
        con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
            .executeQuery("SELECT * FROM ClobParamCodec");
    assertTrue(rs.next());
    assertEquals("e🌟£1", rs.getString(2));
    rs.updateClob(2, new MariaDbClob("f🌟10".getBytes(StandardCharsets.UTF_8)));
    rs.updateRow();
    assertEquals("f🌟10", rs.getString(2));

    assertTrue(rs.next());
    assertNull(rs.getString(2));
    rs.updateClob("t1", new MariaDbClob("f🌟15".getBytes(StandardCharsets.UTF_8)));
    rs.updateRow();
    assertEquals("f🌟15", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟2", rs.getString(2));
    rs.updateClob("t1", (Clob) null);
    rs.updateRow();
    assertNull(rs.getString(2));
    assertTrue(rs.wasNull());

    assertTrue(rs.next());
    assertNull(rs.getString(2));
    rs.updateObject("t1", new MariaDbClob("f🌟56".getBytes(StandardCharsets.UTF_8)), 4);
    rs.updateRow();
    assertEquals("f🌟5", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟3", rs.getString(2));
    rs.updateCharacterStream(2, new StringReader("e🌟789"));
    rs.updateRow();
    assertEquals("e🌟789", rs.getString(2));

    assertTrue(rs.next());
    assertNull(rs.getString(2));
    rs.updateCharacterStream(2, new StringReader("e🌟789"), 4);
    rs.updateRow();
    assertEquals("e🌟7", rs.getString(2));

    assertTrue(rs.next());
    assertNull(rs.getString(2));
    rs.updateCharacterStream(2, new StringReader("e🌟789"), 4L);
    rs.updateRow();
    assertEquals("e🌟7", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟5", rs.getString(2));
    rs.updateCharacterStream("t1", new StringReader("e🌟789"));
    rs.updateRow();
    assertEquals("e🌟789", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟1", rs.getString(2));
    rs.updateCharacterStream("t1", new StringReader("e🌟789"), 4);
    rs.updateRow();
    assertEquals("e🌟7", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟1", rs.getString(2));
    rs.updateCharacterStream("t1", new StringReader("e🌟789"), 4L);
    rs.updateRow();
    assertEquals("e🌟7", rs.getString(2));

    assertTrue(rs.next());
    assertEquals(longDataSb.toString(), rs.getString(2));

    assertTrue(rs.next());
    assertNull(rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟5", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟789", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟8", rs.getString(2));
    assertTrue(rs.next());
    assertEquals(longDataSb.toString(), rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟5", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟789", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟8", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟1", rs.getString(2));
    rs.updateNClob(2, new MariaDbClob("g🌟14".getBytes(StandardCharsets.UTF_8)));
    rs.updateRow();
    assertEquals("g🌟14", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟145", rs.getString(2));
    rs.updateNClob("t1", new MariaDbClob("h🌟14".getBytes(StandardCharsets.UTF_8)));
    rs.updateRow();
    assertEquals("h🌟14", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟789", rs.getString(2));
    rs.updateNCharacterStream("t1", new StringReader("e🌟5789"));
    rs.updateRow();
    assertEquals("e🌟5789", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟8", rs.getString(2));
    rs.updateNCharacterStream("t1", new StringReader("e🌟5789"), 5);
    rs.updateRow();
    assertEquals("e🌟57", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟789", rs.getString(2));
    rs.updateNCharacterStream(2, new StringReader("e🌟5789"));
    rs.updateRow();
    assertEquals("e🌟5789", rs.getString(2));

    assertTrue(rs.next());
    assertEquals("e🌟8", rs.getString(2));
    rs.updateNCharacterStream(2, new StringReader("e🌟5789"), 5);
    rs.updateRow();
    assertEquals("e🌟57", rs.getString(2));

    rs = stmt.executeQuery("SELECT * FROM ClobParamCodec");
    assertTrue(rs.next());
    assertEquals("f🌟10", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("f🌟15", rs.getString(2));
    assertTrue(rs.next());
    assertNull(rs.getString(2));
    assertTrue(rs.wasNull());
    assertTrue(rs.next());
    assertEquals("f🌟5", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟789", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟7", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟7", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟789", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟7", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟7", rs.getString(2));
    assertTrue(rs.next());
    assertEquals(longDataSb.toString(), rs.getString(2));
    assertTrue(rs.next());
    assertNull(rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟5", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟789", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟8", rs.getString(2));
    assertTrue(rs.next());
    assertEquals(longDataSb.toString(), rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟5", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟789", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟8", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("g🌟14", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("h🌟14", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟5789", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟57", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟5789", rs.getString(2));
    assertTrue(rs.next());
    assertEquals("e🌟57", rs.getString(2));
    con.commit();
  }
}
