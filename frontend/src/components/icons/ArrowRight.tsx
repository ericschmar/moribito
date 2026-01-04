import * as solidJs from "solid-js";

const ArrowRight: solidJs.Component<
  solidJs.JSX.SvgSVGAttributes<SVGSVGElement>
> = (props) => {
  const [local, others] = solidJs.splitProps(props, ["class"]);
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      fill="currentColor"
      viewBox="0 0 256 256"
      class={local.class}
      {...others}
    >
      <path d="M221.66,133.66l-72,72a8,8,0,0,1-11.32-11.32L196.69,136H40a8,8,0,0,1,0-16H196.69L138.34,61.66a8,8,0,0,1,11.32-11.32l72,72A8,8,0,0,1,221.66,133.66Z"></path>
    </svg>
  );
};

export default ArrowRight;
