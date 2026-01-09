import Carousel from "~/components/landing/Carousel";
import Topbar from "~/components/landing/Topbar";
import Banner from "~/components/landing/Banner";
import Hero from "~/components/landing/Hero";
import InteractiveFeatureSection from "~/components/landing/InteractiveFeatureSection";
import PricingSection from "~/components/landing/PricingSection";
import DocumentationSection from "~/components/landing/DocumentationSection";
import GridAnimation from "~/components/ui/GridAnimation";

export default function Home() {
  return (
    <div class="min-h-screen">
      <div class="fixed inset-0 -z-10">
        <img
          alt=""
          decoding="async"
          data-nimg="fill"
          class="object-cover opacity-80"
          sizes="100vw"
          src="/images/paper-texture.avif"
          style="position: absolute; height: 100%; width: 100%; inset: 0px; color: transparent;"
        />
      </div>
      <Banner />
      <Topbar />
      <div class="mx-auto w-full max-w-[1920px] px-0 sm:px-6 lg:px-8">
        <main class="flex-1">
          <div class="flex w-full items-center justify-center">
            <div class="w-full max-w-[1500px] min-h-screen flex flex-col">
              <div class="border-x border-border mx-0 sm:mx-4 md:mx-8 lg:mx-24 xl:mx-32">
                <main class="flex w-full flex-col">
                  <Hero />
                  <div class="border-t border-border" />
                  <InteractiveFeatureSection />
                  <div class="border-t border-border" />
                  <Carousel />
                  <div class="border-t border-border" />
                  <PricingSection />
                  <div class="border-t border-border" />
                  <DocumentationSection />
                </main>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
