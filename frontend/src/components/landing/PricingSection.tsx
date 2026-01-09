import { Component } from "solid-js";
import PricingCard from "./PricingCard";
import GridAnimation from "../ui/GridAnimation";

const PricingSection: Component = () => {
  const features = [
    "Unlimited LDAP connections",
    "Advanced query builder",
    "Schema inspection",
    "Secure credential storage",
    "Cross-platform support",
  ];

  return (
    <main id="pricing" class="relative flex w-full flex-col py-4">
      <div class="absolute inset-0">
        <GridAnimation />
      </div>

      {/* Content wrapper */}
      <div class="relative">
        {/* Section Heading */}
        <section class="overflow-hidden bg-transparent w-full border-b border-border py-3.75 px-4">
          <div class="flex flex-col sm:flex-row sm:items-end justify-between relative gap-4 sm:gap-6 max-w-7xl mx-auto">
            <div class="flex-1">
              <h1 class="hero-secondary text-moribito-cherry">
                MORIBITO PRICING
              </h1>
              <div class="mt-4 sm:mt-4">
                <p class="font-mono text-primary text-sm md:text-base max-w-2xl">
                  Simple, transparent pricing for your LDAP needs
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Centered Pricing Card */}
        <div class="flex justify-center pt-6">
          <div class="w-full max-w-[557px]">
            <PricingCard price="$XX" period="per user" features={features} />
          </div>
        </div>
      </div>
    </main>
  );
};

export default PricingSection;
