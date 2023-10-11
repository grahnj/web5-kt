package web5.sdk.crypto

import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.JWK
import java.util.UUID

/**
 * A class for managing cryptographic keys in-memory.
 *
 * `InMemoryKeyManager` is an implementation of [KeyManager] that stores keys in-memory using a mutable map. It provides methods to:
 * - Generate private keys ([generatePrivateKey])
 * - Retrieve public keys ([getPublicKey])
 * - Sign payloads ([sign])
 *
 * ### Example Usage:
 * ```
 * val keyManager = InMemoryKeyManager()
 * val keyID = keyManager.generatePrivateKey(JWSAlgorithm.EdDSA, Curve.Ed25519)
 * val publicKey = keyManager.getPublicKey(keyID)
 * ```
 *
 * ### Notes:
 * - Keys are stored in an in-memory mutable map and will be lost once the application is terminated or the object is garbage-collected.
 * - It is suitable for testing or scenarios where persistent storage of keys is not necessary.
 */
public class InMemoryKeyManager : KeyManager {

  /**
   * An in-memory keystore represented as a flat key-value map, where the key is a key ID.
   */
  private val keyStore: MutableMap<String, JWK> = HashMap()

  /**
   * Generates a private key using specified algorithm and curve, and stores it in the in-memory keyStore.
   *
   * @param algorithm The JWA algorithm identifier.
   * @param curve The elliptic curve. Null for algorithms that do not use elliptic curves.
   * @param options Options for key generation, may include specific parameters relevant to the algorithm.
   * @return The key ID of the generated private key.
   */
  override fun generatePrivateKey(algorithm: Algorithm, curve: Curve?, options: KeyGenOptions?): String {
    val jwk = Crypto.generatePrivateKey(algorithm, curve, options)
    keyStore[jwk.keyID] = jwk

    return jwk.keyID
  }

  /**
   * Computes and returns a public key corresponding to the private key identified by the provided keyAlias.
   *
   * @param keyAlias The alias (key ID) of the private key stored in the keyStore.
   * @return The computed public key as a JWK object.
   * @throws Exception if a key with the provided alias is not found in the keyStore.
   */
  override fun getPublicKey(keyAlias: String): JWK {
    // TODO: decide whether to return null or throw an exception
    val privateKey = keyStore[keyAlias] ?: throw IllegalArgumentException("key with alias $keyAlias not found")
    return Crypto.computePublicKey(privateKey)
  }

  /**
   * Signs a payload using the private key identified by the provided keyAlias.
   *
   * The implementation of this method is not yet provided and invoking it will throw a [NotImplementedError].
   *
   * @param keyAlias The alias (key ID) of the private key stored in the keyStore.
   * @param payload The payload to be signed.
   */
  override fun sign(keyAlias: String, payload: Payload): JWSObject {
    val privateKey = keyStore[keyAlias] ?: throw IllegalArgumentException("key with alias $keyAlias not found")
    val header = JWSHeader.Builder(JWSAlgorithm.ES256K).build()

    val jws = JWSObject(header, payload)
    val signer = DefaultJWSSignerFactory().createJWSSigner(privateKey) as ECDSASigner
    signer.jcaContext.provider = BouncyCastleProviderSingleton.getInstance()
    jws.sign(signer)
    return jws
  }

  override fun import(jwk: JWK): String {
    var kid = jwk.keyID
    if (kid.isNullOrEmpty()) {
      kid = UUID.randomUUID().toString()
    }
    keyStore.putIfAbsent(kid, jwk)
    return kid
  }
}
