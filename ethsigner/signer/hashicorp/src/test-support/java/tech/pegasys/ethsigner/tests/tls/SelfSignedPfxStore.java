/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.ethsigner.tests.tls;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.Period;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tuweni.net.tls.TLS;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * Utility class to generate temporary self-signed certificates in PKCS12 and PEM formats for
 * testing purposes using BouncyCastle APIs.
 *
 * <p>Note: DO NOT USE IN PRODUCTION. The generated stores and files are marked to be deleted on JVM
 * exit.
 *
 * <p>The generated certificate supports SAN extension for multiple DNS and IP addresses
 */
public final class SelfSignedPfxStore {
  private static final char[] DEFAULT_PASSWORD = "changeit".toCharArray();
  private static final String DEFAULT_DN = "CN=localhost";
  private static final String DEFAULT_ALIAS = "test";
  private static final BouncyCastleProvider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();
  private static final boolean IS_CA = true;
  private final Path parentPath;
  private final String alias;
  private final char[] password;
  private final String distinguishedName;
  private final List<String> sanHostNames;
  private final List<String> sanIpAddresses;
  private X509Certificate certificate;
  private KeyPair keyPair;
  private Path keyStore;
  private Path trustStore;
  private Path knownClientsFile;
  private Path passwordFile;
  private Path certificatePath;
  private Path privateKeyPath;

  private SelfSignedPfxStore(
      final Path parentPath,
      final String alias,
      final char[] password,
      final String distinguishedName,
      final List<String> sanHostNames,
      final List<String> sanIpAddresses) {
    this.parentPath = parentPath;
    this.alias = alias;
    this.password = password;
    this.distinguishedName = distinguishedName;
    this.sanHostNames = sanHostNames;
    this.sanIpAddresses = sanIpAddresses;
  }

  public static SelfSignedPfxStore create(final Path parentPath) throws Exception {
    try {
      SelfSignedPfxStore selfSignedPfxStore =
          new SelfSignedPfxStore(
              parentPath,
              DEFAULT_ALIAS,
              DEFAULT_PASSWORD,
              DEFAULT_DN,
              List.of("localhost"),
              List.of("127.0.0.1"));
      selfSignedPfxStore.generateKeyPair();
      selfSignedPfxStore.generateSelfSignedCertificate();
      selfSignedPfxStore.createPemFiles();
      selfSignedPfxStore.createKeyStore();
      selfSignedPfxStore.createTrustStore();
      selfSignedPfxStore.createKnownClientsFile();
      selfSignedPfxStore.createPasswordFile();
      return selfSignedPfxStore;
    } catch (final IOException | GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("DoNotCreateSecureRandomDirectly")
  private void generateKeyPair() throws NoSuchAlgorithmException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048, new SecureRandom());
    this.keyPair = keyPairGenerator.generateKeyPair();
  }

  private void generateSelfSignedCertificate() throws Exception {
    final X500Name issuer = new X500Name(distinguishedName);
    final X500Name subject = new X500Name(distinguishedName);
    final BigInteger serialNumber = new BigInteger(String.valueOf(Instant.now().toEpochMilli()));
    final X509v3CertificateBuilder v3CertificateBuilder =
        new JcaX509v3CertificateBuilder(
            issuer,
            serialNumber,
            Date.from(Instant.now()),
            Date.from(Instant.now().plus(Period.ofDays(90))),
            subject,
            keyPair.getPublic());

    // extensions
    v3CertificateBuilder.addExtension(
        Extension.basicConstraints, true, new BasicConstraints(IS_CA));
    v3CertificateBuilder.addExtension(
        Extension.subjectAlternativeName, false, getSubjectAlternativeNames());

    final ContentSigner contentSigner =
        new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());

    this.certificate =
        new JcaX509CertificateConverter()
            .setProvider(BOUNCY_CASTLE_PROVIDER)
            .getCertificate(v3CertificateBuilder.build(contentSigner));
  }

  private GeneralNames getSubjectAlternativeNames() {
    final List<GeneralName> hostGeneralNames =
        sanHostNames.stream()
            .map(hostName -> new GeneralName(GeneralName.dNSName, hostName))
            .collect(Collectors.toList());
    final List<GeneralName> ipGeneralNames =
        sanIpAddresses.stream()
            .map(ipAddress -> new GeneralName(GeneralName.iPAddress, ipAddress))
            .collect(Collectors.toList());
    final GeneralName[] generalNames =
        Stream.of(hostGeneralNames, ipGeneralNames)
            .flatMap(Collection::stream)
            .toArray(GeneralName[]::new);

    return new GeneralNames(generalNames);
  }

  private void createPemFiles() throws IOException, CertificateEncodingException {
    certificatePath = Files.createTempFile(parentPath, alias, ".crt");
    certificatePath.toFile().deleteOnExit();
    privateKeyPath = Files.createTempFile(parentPath, alias, ".key");
    privateKeyPath.toFile().deleteOnExit();

    try (final BufferedWriter writer = Files.newBufferedWriter(privateKeyPath, UTF_8);
        final PemWriter pemWriter = new PemWriter(writer)) {
      pemWriter.writeObject(new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
    }

    try (final BufferedWriter writer = Files.newBufferedWriter(certificatePath, UTF_8);
        final PemWriter pemWriter = new PemWriter(writer)) {
      pemWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
    }
  }

  private void createKeyStore() throws IOException, GeneralSecurityException {
    this.keyStore = convert(true);
  }

  private void createTrustStore() throws IOException, GeneralSecurityException {
    this.trustStore = convert(false);
  }

  private Path convert(final boolean isKeyStore) throws IOException, GeneralSecurityException {
    final KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(null);
    if (isKeyStore) {
      keyStore.setKeyEntry(alias, keyPair.getPrivate(), password, new Certificate[] {certificate});
    } else {
      keyStore.setCertificateEntry(alias, certificate);
    }

    return saveKeyStore(keyStore);
  }

  private Path saveKeyStore(final KeyStore keyStore)
      throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
    final Path pfxPath = Files.createTempFile(parentPath, alias, ".pfx");
    pfxPath.toFile().deleteOnExit();
    try (final FileOutputStream outputStream = new FileOutputStream(pfxPath.toFile())) {
      keyStore.store(outputStream, password);
    }
    return pfxPath;
  }

  private void createKnownClientsFile() throws IOException, CertificateEncodingException {
    final RDN commonNameRDN = new X500Name(distinguishedName).getRDNs(BCStyle.CN)[0];
    final String commonName = IETFUtils.valueToString(commonNameRDN.getFirst().getValue());
    final String fingerPrint = TLS.certificateHexFingerprint(certificate);

    final Path tempFile = Files.createTempFile(parentPath, alias + "knownClientsFile", ".txt");
    tempFile.toFile().deleteOnExit();
    Files.writeString(tempFile, commonName + " " + fingerPrint);
    this.knownClientsFile = tempFile;
  }

  private void createPasswordFile() throws IOException {
    final Path tempFile = Files.createTempFile(parentPath, alias + "pass", ".txt");
    this.passwordFile = Files.writeString(tempFile, new String(password));
  }

  public Path getKeyStoreFile() {
    return keyStore;
  }

  public Path getTrustStoreFile() {
    return trustStore;
  }

  public Path getKnownClientsFile() {
    return knownClientsFile;
  }

  public Path getPasswordFile() {
    return passwordFile;
  }

  public char[] getPassword() {
    return password;
  }

  public Path getCertificatePath() {
    return certificatePath;
  }

  public Path getPrivateKeyPath() {
    return privateKeyPath;
  }
}