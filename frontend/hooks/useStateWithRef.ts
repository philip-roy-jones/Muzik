import { useState, useRef, useCallback, Dispatch, SetStateAction } from 'react';

function useStateWithRef<T>(initialValue: T): [T, React.MutableRefObject<T>, Dispatch<SetStateAction<T>>] {
  const [state, setState] = useState<T>(initialValue);
  const ref = useRef<T>(initialValue);

  // Function to update both state and ref
  const setStateAndRef = useCallback((value: T | ((prevValue: T) => T)) => {
    setState(value); // Update state, triggering re-renders

    // Update ref with the current state or the callback result, if provided
    ref.current = typeof value === 'function' ? (value as (prevValue: T) => T)(ref.current) : value;
  }, []);

  return [state, ref, setStateAndRef];
}

export default useStateWithRef;
