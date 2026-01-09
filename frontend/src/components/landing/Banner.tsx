import { createSignal, onMount, For, onCleanup } from "solid-js";
import { createStore } from "solid-js/store";

const ROWS = 4;
const COLS = 200;
const TOTAL_CELLS = ROWS * COLS;

const COLORS = [
  "bg-moribito-pink-light",
  "bg-moribito-pink-dark",
  "bg-moribito-pink-medium",
  "bg-moribito-pink",
];

const Banner = () => {
  const [isVisible, setIsVisible] = createSignal(true);
  const [activeCells, setActiveCells] = createStore<
    Record<number, string | undefined>
  >({});

  onMount(() => {
    const handleScroll = () => {
      if (window.scrollY > 0) {
        setIsVisible(false);
      } else {
        setIsVisible(true);
      }
    };

    window.addEventListener("scroll", handleScroll);

    const interval = setInterval(() => {
      const idx = Math.floor(Math.random() * TOTAL_CELLS);
      const color = COLORS[Math.floor(Math.random() * COLORS.length)];

      setActiveCells(idx, color);

      setTimeout(
        () => {
          setActiveCells(idx, undefined);
        },
        1000 + Math.random() * 2000,
      );
    }, 100);

    onCleanup(() => {
      window.removeEventListener("scroll", handleScroll);
      clearInterval(interval);
    });
  });

  return (
    <div
      class="relative h-10 w-full bg-moribito-pink transition-all duration-300 overflow-hidden"
      classList={{
        "opacity-100": isVisible(),
        "opacity-0 h-0": !isVisible(),
      }}
    >
      <div
        class="absolute top-0 left-1/2 -translate-x-1/2"
        style={{
          display: "grid",
          "grid-template-columns": `repeat(${COLS}, 10px)`,
          "grid-template-rows": `repeat(${ROWS}, 10px)`,
          gap: "0px",
        }}
      >
        <For each={Array.from({ length: TOTAL_CELLS })}>
          {(_, i) => {
            const index = i();
            return (
              <div
                class={`border-[0.5px] border-[#e5e5e5] opacity-15 transition-all duration-300 ${
                  activeCells[index]
                    ? `${activeCells[index]} opacity-50`
                    : "bg-transparent"
                }`}
                style={{ width: "10px", height: "10px" }}
              />
            );
          }}
        </For>
      </div>
      <div class="relative z-10 flex items-center justify-center h-full">
        <p class="text-white text-sm font-medium">
          Welcome to Moribito! Check out our new features.
        </p>
      </div>
    </div>
  );
};

export default Banner;
