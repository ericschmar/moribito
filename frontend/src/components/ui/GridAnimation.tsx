import { Component, For, onCleanup, onMount } from "solid-js";
import { createStore } from "solid-js/store";

const ROWS = 7;
const COLS = 59;
const TOTAL_CELLS = ROWS * COLS;

const COLORS = [
  "bg-moribito-yellow",
  "bg-moribito-cherry",
  "bg-moribito-pink",
  "bg-moribito-orange",
];

const GridAnimation: Component = () => {
  const [activeCells, setActiveCells] = createStore<
    Record<number, string | undefined>
  >({});

  onMount(() => {
    const interval = setInterval(() => {
      // Light up a new cell
      const idx = Math.floor(Math.random() * TOTAL_CELLS);
      const color = COLORS[Math.floor(Math.random() * COLORS.length)];

      setActiveCells(idx, color);

      // Fade out after random duration
      setTimeout(
        () => {
          setActiveCells(idx, undefined);
        },
        1000 + Math.random() * 2000,
      );
    }, 100);

    onCleanup(() => clearInterval(interval));
  });

  return (
    <div class="w-full overflow-hidden flex justify-center">
      <div
        class="relative"
        style={{
          display: "grid",
          "grid-template-columns": `repeat(${COLS}, 20px)`,
          "grid-template-rows": `repeat(${ROWS}, 20px)`,
          gap: "0px",
          "max-width": "100%",
        }}
      >
        <For each={Array.from({ length: TOTAL_CELLS })}>
          {(_, i) => {
            const index = i();
            return (
              <div
                class={`border-[0.5px] border-[rgba(255,255,255,0.1)] transition-all duration-300 ${
                  activeCells[index]
                    ? `${activeCells[index]} opacity-50`
                    : "bg-transparent"
                }`}
                style={{ width: "20px", height: "20px" }}
              />
            );
          }}
        </For>
      </div>
    </div>
  );
};

export default GridAnimation;
