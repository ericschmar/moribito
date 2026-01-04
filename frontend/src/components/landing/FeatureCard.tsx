import { Component } from "solid-js";

interface FeatureCardProps {
  category: string;
  title: string;
  subtitle: string;
  imageSrc?: string;
  imageAlt?: string;
  index: number;
}

const LOCATION_PADDING = [
  " md:pr-4 md:pb-4 md:pl-0 md:pt-0",
  " md:pl-4 md:pb-4 md:pr-0 md:pt-0",
  " md:pr-4 md:pt-4 md:pl-0 md:pb-0",
  " md:pl-4 md:pt-4 md:pr-0 md:pb-0",
];

const ICON_COLORS = [
  "greptile-green",
  "greptile-orange",
  "greptile-pink",
  "greptile-yellow",
];

const FeatureCard: Component<FeatureCardProps> = (props) => {
  return (
    <div class="relative">
      <div class={`p-4${LOCATION_PADDING[props.index]}`}>
        <div class="group p-8 flex flex-col relative aspect-square bg-card-bg/50">
          <a
            class={`flex items-center justify-center transition-all text-white bg-${ICON_COLORS[props.index]} hover:bg-${ICON_COLORS[props.index]}/90 w-8 h-8 text-base absolute top-2 right-2`}
            aria-label="Learn more about Get context-aware comments on your PRs"
            href="#documentation"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              fill="currentColor"
              viewBox="0 0 256 256"
              class="rotate-45 transition-transform duration-300 group-hover:rotate-0"
            >
              <path d="M200,64V168a8,8,0,0,1-16,0V83.31L69.66,197.66a8,8,0,0,1-11.32-11.32L172.69,72H88a8,8,0,0,1,0-16H192A8,8,0,0,1,200,64Z"></path>
            </svg>
          </a>
          <div>
            <p class={`badge-small text-${ICON_COLORS[props.index]}`}>
              {`[ ${props.category} ]`}
            </p>
            <h3 class="text-primary">{props.title}</h3>
            <p class="text-tertiary text-label">{props.subtitle}</p>
          </div>
          <div class="w-full flex-1 flex items-end justify-center relative min-h-0">
            <div autoplay="" class="w-full h-full object-contain">
              <img
                src="images/moribito.png"
                width="1350"
                height="1080"
                style="width: 100%; height: 100%; transform: translate3d(0px, 0px, 0px); content-visibility: visible;"
              />
            </div>
          </div>
        </div>
      </div>
      <div class="md:hidden absolute left-0 right-0 bottom-0 h-8 translate-y-4 pointer-events-none">
        <div class="absolute left-0 right-0 top-0 border-t border-dashed border-border"></div>
        <div class="absolute left-0 right-0 bottom-0 border-t border-dashed border-border"></div>
      </div>
    </div>
  );
};

export default FeatureCard;
