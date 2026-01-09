import { Component, For } from "solid-js";
import Button from "~/components/ui/Button";

interface PricingCardProps {
  price: string;
  period: string;
  features: string[];
  ctaText?: string;
  trialNote?: string;
}

const PricingCard: Component<PricingCardProps> = (props) => {
  const ctaText = () => props.ctaText || "Start Free Trial";
  const trialNote = () =>
    props.trialNote || "14 day free trial, no credit card required";

  return (
    <div class="w-full">
      <div class="h-full w-full" style="opacity: 1; transform: none;">
        <div class="relative flex h-full flex-col p-6 py-8 transition-transform hover:scale-[1.01] bg-card-bg/50 before:content-[''] before:absolute before:top-0 before:left-0 before:w-2 before:h-2 before:bg-moribito-cherry after:content-[''] after:absolute after:top-0 after:right-0 after:w-2 after:h-2 after:bg-moribito-cherry border border-greptile-green [border-style:dashed] [border-width:1px] [border-dasharray:8,4]">
          <div class="absolute bottom-0 left-0 w-2 h-2 bg-moribito-cherry"></div>
          <div class="absolute bottom-0 right-0 w-2 h-2 bg-moribito-cherry"></div>
          <div class="mb-6 text-center h-[140px] flex flex-col justify-center items-center">
            <h3 class="badge-small mb-3 text-moribito-cherry whitespace-nowrap">
              [ Personal ]
            </h3>
            <div class="flex items-end justify-center gap-1">
              <div class="flex flex-col sm:flex-row sm:items-end">
                <div class="flex items-end">
                  <span class="text-primary text-2xl sm:text-3xl">$</span>
                  <span class="text-primary text-4xl sm:text-5xl font-semibold tracking-tighter">
                    29.99
                  </span>
                </div>
                <div class="text-secondary text-sm font-mono leading-4 mt-1 sm:mt-0 sm:ml-1 flex items-center gap-1">
                  <span>/one-time</span>
                </div>
              </div>
            </div>
          </div>
          <div class="text-secondary mb-6 flex flex-col gap-3 flex-grow">
            <div class="flex items-center gap-3">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                fill="currentColor"
                viewBox="0 0 256 256"
                class="text-moribito-cherry"
              >
                <path d="M128,24C74.17,24,32,48.6,32,80v96c0,31.4,42.17,56,96,56s96-24.6,96-56V80C224,48.6,181.83,24,128,24Zm80,104c0,9.62-7.88,19.43-21.61,26.92C170.93,163.35,150.19,168,128,168s-42.93-4.65-58.39-13.08C55.88,147.43,48,137.62,48,128V111.36c17.06,15,46.23,24.64,80,24.64s62.94-9.68,80-24.64ZM69.61,53.08C85.07,44.65,105.81,40,128,40s42.93,4.65,58.39,13.08C200.12,60.57,208,70.38,208,80s-7.88,19.43-21.61,26.92C170.93,115.35,150.19,120,128,120s-42.93-4.65-58.39-13.08C55.88,99.43,48,89.62,48,80S55.88,60.57,69.61,53.08ZM186.39,202.92C170.93,211.35,150.19,216,128,216s-42.93-4.65-58.39-13.08C55.88,195.43,48,185.62,48,176V159.36c17.06,15,46.23,24.64,80,24.64s62.94-9.68,80-24.64V176C208,185.62,200.12,195.43,186.39,202.92Z"></path>
              </svg>
              <span class="text-label">Unlimited Connections</span>
            </div>
            <div class="flex items-center gap-3">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                fill="currentColor"
                viewBox="0 0 256 256"
                class="text-moribito-cherry"
              >
                <path d="M181.66,146.34a8,8,0,0,1,0,11.32l-24,24a8,8,0,0,1-11.32-11.32L164.69,152l-18.35-18.34a8,8,0,0,1,11.32-11.32Zm-72-24a8,8,0,0,0-11.32,0l-24,24a8,8,0,0,0,0,11.32l24,24a8,8,0,0,0,11.32-11.32L91.31,152l18.35-18.34A8,8,0,0,0,109.66,122.34ZM216,88V216a16,16,0,0,1-16,16H56a16,16,0,0,1-16-16V40A16,16,0,0,1,56,24h96a8,8,0,0,1,5.66,2.34l56,56A8,8,0,0,1,216,88Zm-56-8h28.69L160,51.31Zm40,136V96H152a8,8,0,0,1-8-8V40H56V216H200Z"></path>
              </svg>
              <span class="text-label">Advanced Query Builder</span>
            </div>
            <div class="flex items-center gap-3">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                fill="currentColor"
                viewBox="0 0 256 256"
                class="text-moribito-cherry"
              >
                <path d="M144,157.68a68,68,0,1,0-71.9,0c-20.65,6.76-39.23,19.39-54.17,37.17a8,8,0,0,0,12.25,10.3C50.25,181.19,77.91,168,108,168s57.75,13.19,77.87,37.15a8,8,0,0,0,12.25-10.3C183.18,177.07,164.6,164.44,144,157.68ZM56,100a52,52,0,1,1,52,52A52.06,52.06,0,0,1,56,100Zm197.66,33.66-32,32a8,8,0,0,1-11.32,0l-16-16a8,8,0,0,1,11.32-11.32L216,148.69l26.34-26.35a8,8,0,0,1,11.32,11.32Z"></path>
              </svg>
              <span class="text-label">Interactive Directory Tree</span>
            </div>
            <div class="flex items-center gap-3">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                fill="currentColor"
                viewBox="0 0 256 256"
                class="text-moribito-cherry"
              >
                <path d="M48,64a8,8,0,0,1,8-8H72V40a8,8,0,0,1,16,0V56h16a8,8,0,0,1,0,16H88V88a8,8,0,0,1-16,0V72H56A8,8,0,0,1,48,64ZM184,192h-8v-8a8,8,0,0,0-16,0v8h-8a8,8,0,0,0,0,16h8v8a8,8,0,0,0,16,0v-8h8a8,8,0,0,0,0-16Zm56-48H224V128a8,8,0,0,0-16,0v16H192a8,8,0,0,0,0,16h16v16a8,8,0,0,0,16,0V160h16a8,8,0,0,0,0-16ZM219.31,80,80,219.31a16,16,0,0,1-22.62,0L36.68,198.63a16,16,0,0,1,0-22.63L176,36.69a16,16,0,0,1,22.63,0l20.68,20.68A16,16,0,0,1,219.31,80Zm-54.63,32L144,91.31l-96,96L68.68,208ZM208,68.69,187.31,48l-32,32L176,100.69Z"></path>
              </svg>
              <span class="text-label">Full Schema Inspection</span>
            </div>
            <div class="flex items-center gap-3">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                fill="currentColor"
                viewBox="0 0 256 256"
                class="text-moribito-cherry"
              >
                <path d="M220.27,158.54a8,8,0,0,0-7.7-.46,20,20,0,1,1,0-36.16A8,8,0,0,0,224,114.69V72a16,16,0,0,0-16-16H171.78a35.36,35.36,0,0,0,.22-4,36.11,36.11,0,0,0-11.36-26.24,36,36,0,0,0-60.55,23.62,36.56,36.56,0,0,0,.14,6.62H64A16,16,0,0,0,48,72v32.22a35.36,35.36,0,0,0-4-.22,36.12,36.12,0,0,0-26.24,11.36,35.7,35.7,0,0,0-9.69,27,36.08,36.08,0,0,0,33.31,33.6,35.68,35.68,0,0,0,6.62-.14V208a16,16,0,0,0,16,16H208a16,16,0,0,0,16-16V165.31A8,8,0,0,0,220.27,158.54ZM208,208H64V165.31a8,8,0,0,0-11.43-7.23,20,20,0,1,1,0-36.16A8,8,0,0,0,64,114.69V72h46.69a8,8,0,0,0,7.23-11.43,20,20,0,1,1,36.16,0A8,8,0,0,0,161.31,72H208v32.23a35.68,35.68,0,0,0-6.62-.14A36,36,0,0,0,204,176a35.36,35.36,0,0,0,4-.22Z"></path>
              </svg>
              <span class="text-label">Modern Desktop GUI</span>
            </div>
          </div>
          <div class="mt-auto">
            <a
              target="_blank"
              class="w-full"
              href="https://app.greptile.com/signup"
            >
              <button class="inline-flex items-center justify-center gap-2 whitespace-nowrap text-sm font-mono font-normal tracking-wider transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 [&amp;_svg]:pointer-events-none [&amp;_svg]:size-4 [&amp;_svg]:shrink-0 [&amp;_svg]:flex-shrink-0 box-border bg-moribito-cherry text-white shadow-sm h-8 px-3.5 py-1.5 sm:h-9 sm:px-4 sm:py-2 w-full relative overflow-hidden group">
                <div class="absolute inset-0 pointer-events-none">
                  <div
                    class="absolute left-0 h-[1px] w-0 bg-white/15 group-hover:w-full transition-all duration-500 ease-out"
                    style="top: 8px; transition-delay: 30ms;"
                  ></div>
                  <div
                    class="absolute left-0 h-[1px] w-0 bg-white/15 group-hover:w-full transition-all duration-500 ease-out"
                    style="top: 16px; transition-delay: 60ms;"
                  ></div>
                  <div
                    class="absolute left-0 h-[1px] w-0 bg-white/15 group-hover:w-full transition-all duration-500 ease-out"
                    style="top: 24px; transition-delay: 90ms;"
                  ></div>
                  <div
                    class="absolute left-0 h-[1px] w-0 bg-white/15 group-hover:w-full transition-all duration-500 ease-out"
                    style="top: 32px; transition-delay: 120ms;"
                  ></div>
                  <div
                    class="absolute left-0 h-[1px] w-0 bg-white/15 group-hover:w-full transition-all duration-500 ease-out"
                    style="top: 40px; transition-delay: 150ms;"
                  ></div>
                  <div
                    class="absolute left-0 h-[1px] w-0 bg-white/15 group-hover:w-full transition-all duration-500 ease-out"
                    style="top: 48px; transition-delay: 180ms;"
                  ></div>
                  <div
                    class="absolute left-0 h-[1px] w-0 bg-white/15 group-hover:w-full transition-all duration-500 ease-out"
                    style="top: 56px; transition-delay: 210ms;"
                  ></div>
                  <div
                    class="absolute left-0 h-[1px] w-0 bg-white/15 group-hover:w-full transition-all duration-500 ease-out"
                    style="top: 64px; transition-delay: 240ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 8px; transition-delay: 220ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 16px; transition-delay: 240ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 24px; transition-delay: 260ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 32px; transition-delay: 280ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 40px; transition-delay: 300ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 48px; transition-delay: 320ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 56px; transition-delay: 340ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 64px; transition-delay: 360ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 72px; transition-delay: 380ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 80px; transition-delay: 400ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 88px; transition-delay: 420ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 96px; transition-delay: 440ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 104px; transition-delay: 460ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 112px; transition-delay: 480ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 120px; transition-delay: 500ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 128px; transition-delay: 520ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 136px; transition-delay: 540ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 144px; transition-delay: 560ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 152px; transition-delay: 580ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 160px; transition-delay: 600ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 168px; transition-delay: 620ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 176px; transition-delay: 640ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 184px; transition-delay: 660ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 192px; transition-delay: 680ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 200px; transition-delay: 700ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 208px; transition-delay: 720ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 216px; transition-delay: 740ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 224px; transition-delay: 760ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 232px; transition-delay: 780ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 240px; transition-delay: 800ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 248px; transition-delay: 820ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 256px; transition-delay: 840ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 264px; transition-delay: 860ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 272px; transition-delay: 880ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 280px; transition-delay: 900ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 288px; transition-delay: 920ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 296px; transition-delay: 940ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 304px; transition-delay: 960ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 312px; transition-delay: 980ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 320px; transition-delay: 1000ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 328px; transition-delay: 1020ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 336px; transition-delay: 1040ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 344px; transition-delay: 1060ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 352px; transition-delay: 1080ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 360px; transition-delay: 1100ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 368px; transition-delay: 1120ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 376px; transition-delay: 1140ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 384px; transition-delay: 1160ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 392px; transition-delay: 1180ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 400px; transition-delay: 1200ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 408px; transition-delay: 1220ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 416px; transition-delay: 1240ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 424px; transition-delay: 1260ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 432px; transition-delay: 1280ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 440px; transition-delay: 1300ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 448px; transition-delay: 1320ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 456px; transition-delay: 1340ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 464px; transition-delay: 1360ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 472px; transition-delay: 1380ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 480px; transition-delay: 1400ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 488px; transition-delay: 1420ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 496px; transition-delay: 1440ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 504px; transition-delay: 1460ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 512px; transition-delay: 1480ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 520px; transition-delay: 1500ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 528px; transition-delay: 1520ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 536px; transition-delay: 1540ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 544px; transition-delay: 1560ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 552px; transition-delay: 1580ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 560px; transition-delay: 1600ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 568px; transition-delay: 1620ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 576px; transition-delay: 1640ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 584px; transition-delay: 1660ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 592px; transition-delay: 1680ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 600px; transition-delay: 1700ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 608px; transition-delay: 1720ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 616px; transition-delay: 1740ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 624px; transition-delay: 1760ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 632px; transition-delay: 1780ms;"
                  ></div>
                  <div
                    class="absolute top-0 w-[1px] h-0 bg-white/15 group-hover:h-full transition-all duration-500 ease-out"
                    style="left: 640px; transition-delay: 1800ms;"
                  ></div>
                </div>
                <span class="relative z-10 inline-flex items-center gap-2 whitespace-nowrap">
                  Start 14 day free-trial
                </span>
              </button>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PricingCard;
