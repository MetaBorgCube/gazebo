LLMC Calling Convention
=======================

## Key Points

* If a function does not accept any arguments, the caller only has to consider the return value to be clobbered.
* If a function does accept arguments, the caller must ensure that the arguments are placed in the correct locations.
  Additionally, these locations must be considered clobbered, including the return value.

## Passing Arguments

Arguments are expected to be placed in the temporary data storage container corresponding to the location of the calee.
Genereally, the NSID of this storage container is constructed as follows, where `{ns}` corresponds to callee's namespace and `{name}` corresponds to callee's full name part.

```
    {ns}.__temp__:{name}
```

As data storage always requires access by a nonempty path, the fixed path base is set to `args` (or, in LLMC terms: `'"args"'`).

Each argument is placed relative to this location, joined by a path Select-element corresponding to the name of the argument.

For example, where the NSID of the function is `myns:path/to/fn` and the argument in question is `x`:

```
    myns.__temp__:path/to/fn '"args" "x"'
```

## Retrieving the Return Value

Functions may return a value, depending on whether this is specified in the signature.
To simplify implementations, the return value is placed at a well-known location, contrary to arguments which are placed at a callee-dependent location.
This location has been decided to be in the data storage container `gzb:common`, accessed by the path `ret`.

Callers must always assume the return value to be clobbered when calling any function.
