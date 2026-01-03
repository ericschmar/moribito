Kotlin Multiplatform Licensing System
Architecture: Stripe + Cloudflare Workers + Keygen (or DIY)

1. Environment Secrets
   Set these in your Cloudflare Worker dashboard:

STRIPE_API_KEY: Your Stripe Secret Key.

STRIPE_WEBHOOK_SECRET: The secret from the Webhook dashboard.

RESEND_API_KEY: API Key from Resend.com.

PRIVATE_KEY_RAW: The hex string of your Ed25519 Private Key.

2. Cloudflare Worker Code
   JavaScript

import Stripe from 'stripe';

export default {
async fetch(request, env) {
const stripe = new Stripe(env.STRIPE_API_KEY, {
httpClient: Stripe.createFetchHttpClient(),
});

    const signature = request.headers.get('stripe-signature');
    const body = await request.text();

    try {
      const event = stripe.webhooks.constructEvent(body, signature, env.STRIPE_WEBHOOK_SECRET);

      if (event.type === 'checkout.session.completed') {
        const session = event.data.object;
        const email = session.customer_details.email;
        
        // Generate License
        const licenseKey = await generateSignedKey(email, env.PRIVATE_KEY_RAW);

        // Store for future migration
        await env.LICENSE_STORAGE.put(`license:${email}`, JSON.stringify({ key: licenseKey }));

        // Send via Resend
        await fetch("https://api.resend.com/emails", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${env.RESEND_API_KEY}`
          },
          body: JSON.stringify({
            from: "Licensing <licensing@yourdomain.com>",
            to: [email],
            subject: "Your App License Key",
            html: `<p>Your key: <code>${licenseKey}</code></p>`
          })
        });
      }
      return new Response("OK", { status: 200 });
    } catch (err) {
      return new Response(err.message, { status: 400 });
    }
}
};

async function generateSignedKey(email, privateKeyRaw) {
    const payload = `${email}|2099-12-31`;
    let privKeyBuffer;

    if (privateKeyRaw.startsWith('MC4CAQAw')) {
        // Base64 PKCS#8
        const binaryString = atob(privateKeyRaw);
        privKeyBuffer = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            privKeyBuffer[i] = binaryString.charCodeAt(i);
        }
    } else if (/^[0-9a-fA-F]{64}$/.test(privateKeyRaw)) {
        // 64-char Hex (32 bytes raw key)
        const rawBuffer = new Uint8Array(privateKeyRaw.match(/.{1,2}/g).map(byte => parseInt(byte, 16)));
        const pkcs8Header = new Uint8Array([0x30, 0x2e, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x04, 0x22, 0x04, 0x20]);
        privKeyBuffer = new Uint8Array(pkcs8Header.length + rawBuffer.length);
        privKeyBuffer.set(pkcs8Header);
        privKeyBuffer.set(rawBuffer, pkcs8Header.length);
    } else {
        throw new Error("Invalid private key format. Expected 64-char hex or Base64 PKCS#8.");
    }

    const privateKey = await crypto.subtle.importKey("pkcs8", privKeyBuffer, "Ed25519", false, ["sign"]);
    const sig = await crypto.subtle.sign("Ed25519", privateKey, new TextEncoder().encode(payload));

    const toBase64Url = (strOrBuf) => {
        let binary = '';
        if (typeof strOrBuf === 'string') {
            binary = strOrBuf;
        } else {
            const bytes = new Uint8Array(strOrBuf);
            for (let i = 0; i < bytes.byteLength; i++) {
                binary += String.fromCharCode(bytes[i]);
            }
        }
        return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    };

    return `${toBase64Url(payload)}.${toBase64Url(sig)}`;
}
3. Kotlin Verification (JVM/Desktop)
   Kotlin

object LicenseVerifier {
private const val PUBLIC_KEY_B64 = "YOUR_PUBLIC_KEY_HERE"

    fun verify(licenseKey: String): Boolean {
        return try {
            val parts = licenseKey.split(".")
            val data = Base64.getUrlDecoder().decode(parts[0])
            val sig = Base64.getUrlDecoder().decode(parts[1])
            
            val pubKey = KeyFactory.getInstance("Ed25519")
                .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY_B64)))

            Signature.getInstance("Ed25519").apply {
                initVerify(pubKey)
                update(data)
            }.verify(sig)
        } catch (e: Exception) { false }
    }
}