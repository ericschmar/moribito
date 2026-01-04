import { Component, JSX } from 'solid-js';

interface ContainerProps {
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  children: JSX.Element;
  class?: string;
}

const maxWidthClasses = {
  'sm': 'max-w-screen-sm',
  'md': 'max-w-screen-md',
  'lg': 'max-w-screen-lg',
  'xl': 'max-w-7xl', // 1280px
  '2xl': 'max-w-screen-2xl',
};

const Container: Component<ContainerProps> = (props) => {
  const maxWidth = () => props.maxWidth || 'xl';
  const maxWidthClass = () => maxWidthClasses[maxWidth()];

  return (
    <div class={`${maxWidthClass()} mx-auto px-4 md:px-8 ${props.class || ''}`}>
      {props.children}
    </div>
  );
};

export default Container;
