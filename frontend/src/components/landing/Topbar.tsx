import { Component } from "solid-js";
import Button from "~/components/ui/Button";
import FeatureStars from "../icons/FeatureStars";
import Resources from "../icons/Resources";
import DollarSign from "../icons/DollarSign";

const Topbar: Component = () => {
  return (
    <div class="sticky top-0 z-50 w-full">
      <nav class="w-full transition-all duration-300 bg-background border-b border-border">
        <div class="mx-auto w-full max-w-[1550px]">
          <div class="flex h-14 items-center justify-between mx-4 sm:mx-12 md:mx-12 lg:mx-32 xl:mx-40">
            {/* Logo */}
            <div class="flex items-center">
              <div class="w-[120px] h-[40px] flex items-center justify-center text-sm font-semibold text-[rgb(42,42,42)]">
                <img
                  src="/images/moribito_trans.png"
                  alt="Moribito Logo"
                  class="w-8 h-8 mr-2"
                />
                Moribito
              </div>
            </div>

            {/* Navigation Links (hidden on mobile) */}
            <div class="hidden md:flex items-center gap-8">
              <a
                href="#features"
                class="text-sm font-mono text-primary hover:text-secondary transition-colors flex items-center gap-1.5"
              >
                <FeatureStars />
                Features
              </a>
              <div class="hidden lg:block h-4 w-px border-r border-dashed border-border mx-1.5 opacity-50" />
              <a
                href="#documentation"
                class="text-sm font-mono text-primary hover:text-secondary transition-colors flex items-center gap-1.5"
              >
                <Resources />
                Documentation
              </a>
              <div class="hidden lg:block h-4 w-px border-r border-dashed border-border mx-1.5 opacity-50" />
              <a
                href="#pricing"
                class="text-sm font-mono text-primary hover:text-secondary transition-colors flex items-center gap-1.5"
              >
                <DollarSign />
                Pricing
              </a>
            </div>

            {/* Download Button */}
            <Button variant="outline-large" disabled class="max-w-[150px]">
              Download
            </Button>
          </div>
        </div>
      </nav>
    </div>
  );
};

export default Topbar;
