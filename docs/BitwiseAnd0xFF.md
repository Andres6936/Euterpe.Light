# Bitwise-ANDing with 0xff is important?

`0xff` is just `1111 1111`. But this is attempting to display the unsigned
byte value, even though in Java `byte`s are signed. The value `0xff` is `-1`
for a signed `byte`, but it's `255` in a `short`.

When a `byte` value of `0xff` is read, printing the value would yield `-1`. 
So it's assigned to a `short` which has a bigger range and can store `byte` 
values that would normally overflow to be a negative number as a `byte` as
a positive integer, e.g. 144 as a `byte` is `0x90`, or -112, but it can be
properly stored as `144` as a `short`.

So the `byte` value of `-1` is assigned to a `short`. But what does that do? 
A primitive widening conversion takes place, and negative values are 
sign-extended. So `1111 1111` becomes `11111111 11111111`, still `-1`, but 
this time as a short.

Then the bitmask `0xff` (`00000000 11111111`) is used to get the last 8
bits out again:

```
  -1: 11111111 1111111
0xFF: 00000000 1111111
======================
255: 00000000 1111111
```

It's just a way to get the unsigned `byte` value, by converting it to 
`short` and then masking out the original bits from the `byte`, to display
it as an unsigned value.

###### Reference: https://stackoverflow.com/a/19061591