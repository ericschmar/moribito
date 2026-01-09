import { Component, createSignal, createEffect, For } from "solid-js";

interface Feature {
  category: string;
  title: string;
  description: string;
  imageSrc: string;
  imageAlt: string;
}

const InteractiveFeatureSection: Component = () => {
  const features: Feature[] = [
    {
      category: "NAVIGATION",
      title: "Browse Directory Trees",
      description:
        "Explore your LDAP directory structure with an intuitive tree view. Navigate through organizational units, containers, and objects effortlessly.",
      imageSrc: "/images/tree.png",
      imageAlt: "Browse Directory Trees",
    },
    {
      category: "SEARCH",
      title: "Advanced Query Builder",
      description:
        "Build complex LDAP queries with ease. The only viewer on the market with a SQL-like query builder.",
      imageSrc: "/images/sql_query.png",
      imageAlt: "Advanced Query Builder",
    },
    {
      category: "DISCOVERY",
      title: "Schema Inspection",
      description:
        "Discover and explore your LDAP schema. View object classes, attribute types, and syntaxes with detailed documentation.",
      imageSrc: "/images/attributes.png",
      imageAlt: "Schema Inspection",
    },
    {
      category: "SECURITY",
      title: "Connection Management",
      description:
        "Manage multiple LDAP connections with secure credential storage. Support for LDAPS, STARTTLS, and various bind mechanisms.",
      imageSrc: "/images/connections.png",
      imageAlt: "Connection Management",
    },
  ];

  const [activeIndex, setActiveIndex] = createSignal(0);
  const [isAutoPlaying, setIsAutoPlaying] = createSignal(true);
  const [fadeIn, setFadeIn] = createSignal(true);

  // Auto-cycle through features every 5 seconds
  createEffect(() => {
    if (!isAutoPlaying()) return;

    const interval = setInterval(() => {
      setActiveIndex((prev) => (prev + 1) % features.length);
    }, 5000);

    return () => clearInterval(interval);
  });

  const handleFeatureClick = (index: number) => {
    if (activeIndex() !== index) {
      setFadeIn(false);
      setTimeout(() => {
        setActiveIndex(index);
        setFadeIn(true);
      }, 250);
    }
    setIsAutoPlaying(false);
  };

  const handleMouseEnter = () => {
    setIsAutoPlaying(false);
  };

  const handleMouseLeave = () => {
    setIsAutoPlaying(true);
  };

  const activeFeature = () => features[activeIndex()];

  return (
    <section class="w-full">
      <div class="hidden md:block w-full">
        <div class="relative border-t border-b border-dashed border-border px-4 md:px-8 py-8">
          <div class="relative">
            <div class="relative grid gap-0 grid-cols-1 md:grid-cols-2 lg:grid-cols-2">
              {/* Left Section */}
              <div
                class="flex flex-col justify-center space-y-6 p-8"
                onMouseEnter={handleMouseEnter}
                onMouseLeave={handleMouseLeave}
              >
                <div>
                  <h2 class="font-title text-4xl font-semibold text-moribito-cherry mb-2">
                    Powerful Features
                  </h2>
                  <p class="text-tertiary text-label">
                    Everything you need to manage LDAP directories
                  </p>
                </div>

                {/* Features List */}
                <div class="space-y-4">
                  <For each={features}>
                    {(feature, index) => (
                      <button
                        onClick={() => handleFeatureClick(index())}
                        class={`w-full text-left p-4 transition-opacity duration-300 cursor-pointer ${
                          activeIndex() === index()
                            ? "opacity-100"
                            : "opacity-40 hover:opacity-60"
                        }`}
                      >
                        <div class="flex flex-col gap-1">
                          <div class="flex items-baseline gap-2">
                            <p class="badge-small text-moribito-pink">
                              [ {feature.category} ]
                            </p>
                            <h3 class="text-primary font-semibold">
                              {feature.title}
                            </h3>
                          </div>
                          <p
                            class={`text-label transition-all duration-300 ${
                              activeIndex() === index()
                                ? "text-tertiary opacity-100 h-auto"
                                : "text-tertiary opacity-0 h-0 overflow-hidden"
                            }`}
                          >
                            {feature.description}
                          </p>
                        </div>
                      </button>
                    )}
                  </For>
                </div>
              </div>

              {/* Right Section - Screenshot */}
              <div class="relative flex items-center justify-center p-8">
                <div class="relative w-full aspect-square max-w-md">
                  {/* Grid Background */}
                  <div class="absolute inset-0 bg-gradient-to-br from-orange-900/10 via-orange-950/5 to-transparent rounded-lg overflow-hidden">
                    <div
                      class="absolute inset-0"
                      style={{
                        "background-image":
                          "linear-gradient(90deg, rgba(255, 139, 70, 0.05) 1px, transparent 1px), linear-gradient(rgba(255, 139, 70, 0.05) 1px, transparent 1px)",
                        "background-size": "24px 24px",
                      }}
                    ></div>
                  </div>

                  {/* Screenshot Container with Fade Transition */}
                  <div class="absolute inset-0 flex items-center justify-center py-4 px-4 rounded-lg">
                    <img
                      src={activeFeature().imageSrc}
                      alt={activeFeature().imageAlt}
                      class={`w-full h-full object-contain transition-opacity duration-500 rounded-lg ${
                        fadeIn() ? "opacity-100" : "opacity-0"
                      }`}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default InteractiveFeatureSection;
