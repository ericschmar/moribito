import type { Component } from "solid-js";
import Button from "~/components/ui/Button";
import ArrowRight from "~/components/icons/ArrowRight";

const Hero: Component = () => {
  return (
    <section class="h-full w-full">
      <div class="relative min-h-[80vh] w-full pt-12 sm:pt-16 overflow-hidden">
        <div class="absolute left-4 sm:left-8 top-12 sm:top-16 flex flex-col items-start">
          <h1 class="text-moribito-cherry">Your LDAP</h1>
          <h1 class="text-moribito-cherry">Viewer</h1>
        </div>

        <div class="absolute right-0 bottom-0 h-[500px] w-[500px] sm:h-[600px] sm:w-[600px] xl:h-[750px] xl:w-[750px] overflow-visible">
          <div
            class="lottie h-full w-full opacity-100 sm:opacity-100"
            style="object-position: right bottom;"
          >
            <div style="width: 100%; height: 100%;">
              <img
                src="/images/moribito_trans.png"
                alt="Moribito LDAP Viewer"
                width={1080}
                height={1080}
                style="width: 100%; height: 100%; transform: translate3d(0px, 0px, 0px); content-visibility: visible;"
              />
            </div>
          </div>
        </div>

        <div class="absolute bottom-8 left-4 sm:left-8 flex flex-col items-start text-left">
          <p class="font-mono text-moribito-cherry">EXPLORE AND MANAGE</p>
          <p class="font-mono text-moribito-cherry">YOUR LDAP DATA WITH EASE</p>
          <div class="mt-6 flex w-full max-w-[280px] flex-col items-start">
            <Button variant="primary-small" trailingIcon={<ArrowRight />}>
              Try for Free
            </Button>
            <p class="text-[11px] text-tertiary font-mono whitespace-nowrap">
              14 day free trial, no credit card required
            </p>
          </div>
        </div>
      </div>
    </section>
  );
};

export default Hero;
