import { Component, JSX, splitProps } from "solid-js";

interface ButtonProps extends JSX.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?:
    | "primary-large"
    | "primary-small"
    | "outline-small"
    | "outline-large";
  href?: string;
  leadingIcon?: JSX.Element;
  trailingIcon?: JSX.Element;
  children: JSX.Element;
}

const buttonVariants = {
  "primary-large":
    "h-14 min-w-full md:min-w-[557px] px-0 py-4 text-[36px] font-semibold bg-moribito-cherry text-white rounded-none",
  "primary-small":
    "h-5 min-w-[111px] px-0 py-0 text-sm font-normal bg-moribito-cherry",
  "outline-small":
    "h-9 px-4 py-2 text-sm font-normal bg-[rgb(220,220,220)] text-[rgb(24,24,26)] rounded-none shadow-[rgba(0,0,0,0)_0px_0px_0px_0px,rgba(0,0,0,0)_0px_0px_0px_0px,rgba(0,0,0,0.05)_0px_1px_2px_0px]",
  "outline-large":
    "h-9 px-4 py-2 text-sm font-normal bg-[rgb(26,142,97)] text-white rounded-none shadow-[rgba(0,0,0,0)_0px_0px_0px_0px,rgba(0,0,0,0)_0px_0px_0px_0px,rgba(0,0,0,0.05)_0px_1px_2px_0px]",
};

const buttonHoverVariants = {
  "primary-large": "hover:opacity-90",
  "primary-small": "hover:opacity-90",
  "outline-small":
    "hover:bg-[rgba(16,122,77,0.2)] hover:border-[rgba(16,122,77,0.5)]",
  "outline-large":
    "hover:bg-[rgba(16,122,77,0.2)] hover:border-[rgba(16,122,77,0.5)]",
};

const Button: Component<ButtonProps> = (props) => {
  const [local, others] = splitProps(props, [
    "variant",
    "href",
    "children",
    "class",
    "leadingIcon",
    "trailingIcon",
  ]);
  const variant = () => local.variant || "primary-large";

  const baseClasses =
    "transition-custom disabled:pointer-events-none disabled:opacity-50 inline-flex items-center justify-center";
  const variantClass = () => buttonVariants[variant()];
  const hoverClass = () => buttonHoverVariants[variant()];

  // If href is provided, render as an anchor tag
  if (local.href) {
    return (
      <a
        href={local.href}
        class="inline-flex items-center justify-center gap-2 whitespace-nowrap text-sm font-mono font-normal tracking-wider transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0 [&_svg]:flex-shrink-0 box-border bg-moribito-cherry text-white shadow-sm h-8 px-3.5 py-1.5 sm:h-9 sm:px-4 sm:py-2 w-full relative overflow-hidden group"
      >
        {local.leadingIcon}
        {local.children}
        {local.trailingIcon}
      </a>
    );
  }

  // Otherwise render as a button
  return (
    <button
      class={`${local.class} inline-flex items-center justify-center gap-2 whitespace-nowrap text-sm font-mono font-normal tracking-wider transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0 [&_svg]:flex-shrink-0 box-border bg-moribito-cherry text-white shadow-sm h-8 px-3.5 py-1.5 sm:h-9 sm:px-4 sm:py-2 w-full relative overflow-hidden group`}
      {...others}
    >
      {local.leadingIcon}
      {local.children}
      {local.trailingIcon}
    </button>
  );
};

export default Button;
