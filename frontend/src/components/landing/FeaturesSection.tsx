import { Component } from "solid-js";
import Container from "~/components/ui/Container";
import FeatureCard from "./FeatureCard";

const FeaturesSection: Component = () => {
  const features = [
    {
      category: "NAVIGATION",
      title: "Browse Directory Trees",
      subtitle:
        "Explore your LDAP directory structure with an intuitive tree view. Navigate through organizational units, containers, and objects effortlessly.",
      imageSrc: "/images/feature-tree.svg",
      imageAlt: "Browse Directory Trees",
    },
    {
      category: "SEARCH",
      title: "Advanced Query Builder",
      subtitle:
        "Build complex LDAP queries with ease. Support for filters, search scopes, and attribute selection with syntax highlighting.",
      imageSrc: "/images/feature-query.svg",
      imageAlt: "Advanced Query Builder",
    },
    {
      category: "DISCOVERY",
      title: "Schema Inspection",
      subtitle:
        "Discover and explore your LDAP schema. View object classes, attribute types, and syntaxes with detailed documentation.",
      imageSrc: "/images/feature-schema.svg",
      imageAlt: "Schema Inspection",
    },
    {
      category: "SECURITY",
      title: "Connection Management",
      subtitle:
        "Manage multiple LDAP connections with secure credential storage. Support for LDAPS, STARTTLS, and various bind mechanisms.",
      imageSrc: "/images/feature-connection.svg",
      imageAlt: "Connection Management",
    },
  ];

  return (
    <section class="w-full">
      <div class="hidden md:block w-full">
        <div class="relative border-t border-b border-dashed border-border px-4 md:px-8 py-8">
          <div class="relative">
            <div class="hidden md:block absolute top-0 -left-8 -right-8 border-t border-dashed border-border"></div>
            <div class="hidden md:block absolute bottom-0 -left-8 -right-8 border-b border-dashed border-border"></div>
            <div class="hidden md:block absolute left-0 -top-8 -bottom-8 border-l border-dashed border-border"></div>
            <div class="hidden md:block absolute right-0 -top-8 -bottom-8 border-r border-dashed border-border"></div>

            <div class="relative grid gap-0 grid-cols-1 md:grid-cols-2 lg:grid-cols-2">
              <div class="hidden md:block absolute left-1/2 -top-8 -bottom-8 w-8 -translate-x-1/2 pointer-events-none z-10">
                <div class="absolute left-0 top-0 bottom-0 border-l border-dashed border-border"></div>
                <div class="absolute right-0 top-0 bottom-0 border-l border-dashed border-border"></div>
              </div>
              <div class="hidden md:block absolute -left-8 -right-8 top-1/2 h-8 -translate-y-1/2 pointer-events-none z-10">
                <div class="absolute left-0 right-0 top-0 border-t border-dashed border-border"></div>
                <div class="absolute left-0 right-0 bottom-0 border-t border-dashed border-border"></div>
              </div>
              {features.map((feature, index) => (
                <FeatureCard
                  index={index}
                  category={feature.category}
                  title={feature.title}
                  subtitle={feature.subtitle}
                  imageSrc={feature.imageSrc}
                  imageAlt={feature.imageAlt}
                />
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default FeaturesSection;
