import { useState } from 'react';
import type { DragEvent } from 'react';

export function useDragSort<T>(items: T[], setItems: (next: T[]) => void) {
  const [dragIndex, setDragIndex] = useState<number | null>(null);

  function handlers(index: number) {
    return {
      draggable: true,
      onDragStart: () => setDragIndex(index),
      onDragOver: (event: DragEvent<HTMLElement>) => {
        event.preventDefault();
        if (dragIndex === null || dragIndex === index) {
          return;
        }
        const next = [...items];
        const [moved] = next.splice(dragIndex, 1);
        next.splice(index, 0, moved);
        setDragIndex(index);
        setItems(next);
      },
      onDragEnd: () => setDragIndex(null),
      onDrop: () => setDragIndex(null),
    };
  }

  return { handlers, dragging: dragIndex !== null };
}

export function moveItem<T>(items: T[], index: number, direction: -1 | 1): T[] {
  const target = index + direction;
  if (target < 0 || target >= items.length) {
    return items;
  }
  const next = [...items];
  [next[index], next[target]] = [next[target], next[index]];
  return next;
}
