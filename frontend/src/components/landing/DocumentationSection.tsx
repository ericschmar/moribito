import { Component } from "solid-js";
import Container from "~/components/ui/Container";

const DocumentationSection: Component = () => {
  const docLinks = [
    { title: "Getting Started", href: "#" },
    { title: "User Guide", href: "#" },
    { title: "API Reference", href: "#" },
  ];

  return (
    <section id="documentation" class="py-16 lg:py-40">
      <Container>
        {/* Section Heading */}
        <h2 class="text-[36px] leading-[40px] lg:text-[48px] lg:leading-[48px] font-semibold text-[rgb(42,42,42)] mb-8 text-center font-title">
          Documentation
        </h2>

        {/* Introduction Text */}
        <div class="max-w-3xl mx-auto text-center mb-12">
          <p class="text-[18px] leading-[28px] text-[rgb(42,42,42)] mb-4">
            Coming soon!
          </p>
        </div>
      </Container>
    </section>
  );
};

export default DocumentationSection;
