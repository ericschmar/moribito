import { Component } from 'solid-js';
import Container from '~/components/ui/Container';

const DocumentationSection: Component = () => {
  const docLinks = [
    { title: 'Getting Started', href: '#' },
    { title: 'User Guide', href: '#' },
    { title: 'API Reference', href: '#' },
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
            Get started with Moribito quickly with our comprehensive documentation.
            Learn how to connect to your LDAP server, run queries, and explore your
            directory structure.
          </p>
        </div>

        {/* Placeholder Links */}
        <div class="flex flex-wrap justify-center gap-6">
          {docLinks.map((link) => (
            <a
              href={link.href}
              class="inline-block px-6 py-3 text-[16px] font-normal text-[rgb(42,42,42)] border border-[rgb(42,42,42)] hover:bg-[rgb(42,42,42)] hover:text-white transition-custom"
            >
              {link.title}
            </a>
          ))}
        </div>
      </Container>
    </section>
  );
};

export default DocumentationSection;
