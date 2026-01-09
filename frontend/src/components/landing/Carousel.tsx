import { createSignal, onCleanup, For } from "solid-js";

const Carousel = () => {
  const images = [
    "/carousel/start.png",
    "/carousel/connections.png",
    "/carousel/workspace.png",
  ];

  const [currentIndex, setCurrentIndex] = createSignal(0);

  const interval = setInterval(() => {
    setCurrentIndex((prevIndex) =>
      prevIndex === images.length - 1 ? 0 : prevIndex + 1,
    );
  }, 3000);

  onCleanup(() => clearInterval(interval));

  return (
    <div class="relative w-full max-w-2xl mx-auto overflow-hidden rounded-lg py-4">
      <h2 class="font-title text-4xl font-semibold text-moribito-cherry mb-2">
        Screenshots
      </h2>
      <div
        class="flex transition-transform duration-500 ease-in-out"
        style={{ transform: `translateX(-${currentIndex() * 100}%)` }}
      >
        <For each={images}>
          {(image, index) => (
            <img
              src={image}
              alt={`Slide ${index()}`}
              class="w-full h-auto shrink-0 rounded-lg"
            />
          )}
        </For>
      </div>
    </div>
  );
};

export default Carousel;
