package it.gov.pagopa.notification.manager.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class AESUtilTest {
  private static final String CIPHER_INSTANCE = "AES/GCM/NoPadding";
  private static final String ENCODING = "UTF-8";
  public static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA1";
  private static final String SALT = "SALT_SAMPLE";
  private static final int KEY_SIZE = 256;
  private static final int ITERATION_COUNT = 10000;
  private static final String PLAIN_TEXT = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy ";
  private static final String PASSPHRASE = "the quick brown fox jumps over the lazy dog";
  private static final String GCM_IV = "IV_SAMPLE";
  private static final int GCM_TAG_LENGTH = 16;
  private static final String CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000 = "wzQD/tLo8vghu5EvhZYDaUnp1B5x1e1pBZJHTLkUlNYLGh/rrzkGgFMtrjZUEgOsZoKfJ5gL4DWQN209PooN0kwq6XTt1Kuj5NF2tvyO5yadXmqvV0DbRIQ";
  private AESUtil util;

  @Test
  void testDecrypt() {
    util = new AESUtil(CIPHER_INSTANCE, ENCODING, PBE_ALGORITHM, SALT, KEY_SIZE, ITERATION_COUNT, GCM_IV, GCM_TAG_LENGTH);
    String decrypt = util.decrypt(PASSPHRASE, CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000);
    assertEquals(PLAIN_TEXT, decrypt);
  }

  @Test
  void testGenerateKey_throwInvalidKeyException() {
    util = new AESUtil(CIPHER_INSTANCE, ENCODING, PBE_ALGORITHM, SALT, 89, 18, GCM_IV,
        GCM_TAG_LENGTH);
    Executable executable = () -> util.decrypt(PASSPHRASE, CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000);
    Assertions.assertThrows(IllegalStateException.class, executable);
  }

  @Test
  void testGenerateKey_throwNoSuchAlgorithmException(){
    util = new AESUtil(CIPHER_INSTANCE, ENCODING, "ALGORITHM_INVALID", SALT, KEY_SIZE,
        ITERATION_COUNT, GCM_IV, GCM_TAG_LENGTH);
    Executable executable = () -> util.decrypt(PASSPHRASE, CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000);
    Assertions.assertThrows(IllegalStateException.class, executable);
  }

    @Test
    void testDecrypt_throwUnsupportedEncodingException(){
      util = new AESUtil(CIPHER_INSTANCE, "ENCODING_NOT_VALID", PBE_ALGORITHM, SALT, KEY_SIZE,
          ITERATION_COUNT, GCM_IV, GCM_TAG_LENGTH);
        Executable executable = () -> util.decrypt(PASSPHRASE, CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000);
        Assertions.assertThrows(IllegalStateException.class, executable);
    }

  @Test
  void AESUtil_throwNoSuchAlgorithmException() {
    util = new AESUtil("INSTANCE_NOT_VALID", ENCODING, PBE_ALGORITHM, SALT, KEY_SIZE,
        ITERATION_COUNT, GCM_IV, GCM_TAG_LENGTH);
    Executable executable = () -> util.decrypt(PASSPHRASE, CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000);
    Assertions.assertThrows(IllegalStateException.class, executable);
  }

  @Test
  void AESUtil_throwNoSuchPaddingException() {
    util = new AESUtil("AES/GCM", ENCODING, PBE_ALGORITHM, SALT, KEY_SIZE, ITERATION_COUNT, GCM_IV,
        GCM_TAG_LENGTH);
    Executable executable = () -> util.decrypt(PASSPHRASE, CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000);
    Assertions.assertThrows(IllegalStateException.class, executable);
  }
}
