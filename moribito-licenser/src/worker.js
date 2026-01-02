import Stripe from 'stripe';

export default {
  async fetch(request, env) {
    console.log('Received request:', request.method, request.url);

    const stripe = new Stripe(env.STRIPE_API_KEY, {
      httpClient: Stripe.createFetchHttpClient(),
    });

    const signature = request.headers.get('stripe-signature');
    const body = await request.text();

    console.log('Stripe signature present:', !!signature);

    try {
      const event = await stripe.webhooks.constructEventAsync(body, signature, env.STRIPE_WEBHOOK_SECRET);
      console.log('Webhook event received:', event.type);

      if (event.type === 'checkout.session.completed') {
        console.log('Processing checkout.session.completed event');
        const session = event.data.object;
        const email = session.customer_details.email;
        const name = session.customer_details.name || "Customer";

        console.log('Customer email:', email);

        // 1. Generate the License Key (using the logic from previous steps)
        const licenseKey = await generateSignedKey(email, env.PRIVATE_KEY_RAW);
        console.log('License key generated');

        // 2. Store in KV (skip if not available in dev)
        if (env.LICENSE_STORAGE) {
          await env.LICENSE_STORAGE.put(`license:${email}`, JSON.stringify({ key: licenseKey }));
        } else {
          console.log('Skipping KV storage (not available in dev mode)');
        }

        // 3. Send Email via Resend using template
        const emailResponse = await fetch("https://api.resend.com/emails", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${env.RESEND_API_KEY}`
          },
          body: JSON.stringify({
            from: "Moribito <licenses@moribito.cc>",
            to: [email],
            template: {
              id: "license",
              variables: {
                licenseKey: licenseKey,
                name: name
              }
            }
          })
        });

        if (!emailResponse.ok) {
          const errorText = await emailResponse.text();
          console.error('Resend error:', errorText);
          throw new Error("Email failed to send");
        }
      }

      return new Response("Success", { status: 200 });
    } catch (err) {
      console.error('Error processing webhook:', err.message);
      console.error('Full error:', err);
      return new Response(`Error: ${err.message}`, { status: 400 });
    }
  }
};

// Helper to sign the key (Ed25519)
async function generateSignedKey(email, privateKeyHex) {
  const payload = `${email}|2099-12-31`;
  const privKeyBuffer = hexToUint8Array(privateKeyHex);

  // Wrap raw 32-byte Ed25519 key in PKCS#8 format for WebCrypto
  const pkcs8Header = new Uint8Array([
    0x30, 0x2e, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06,
    0x03, 0x2b, 0x65, 0x70, 0x04, 0x22, 0x04, 0x20
  ]);
  const pkcs8Key = new Uint8Array(pkcs8Header.length + privKeyBuffer.length);
  pkcs8Key.set(pkcs8Header);
  pkcs8Key.set(privKeyBuffer, pkcs8Header.length);

  const privateKey = await crypto.subtle.importKey("pkcs8", pkcs8Key, "Ed25519", false, ["sign"]);
  const sig = await crypto.subtle.sign("Ed25519", privateKey, new TextEncoder().encode(payload));

  return `${btoa(payload)}.${btoa(String.fromCharCode(...new Uint8Array(sig)))}`
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function hexToUint8Array(hex) {
  return new Uint8Array(hex.match(/.{1,2}/g).map(byte => parseInt(byte, 16)));
}
