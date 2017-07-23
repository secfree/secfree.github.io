---
layout: blog
title: "Questions about Logistic Regression in 'Machine Learning in Action' and Full Explanation"
---

# 1. The symbols mean of the gradient function?

$$
  \Delta f(x,y) =
  \begin{pmatrix}
    \frac {\partial f(x,y)}{\partial x} \\
    \frac {\partial f(x,y)}{\partial y}
  \end{pmatrix}
$$

<br />

1. We must recall the meaning of [Derivative](http://www.mathsisfun.com/calculus/derivatives-introduction.html) and [Partial derivative](https://en.wikipedia.org/wiki/Partial_derivative)
1. `y` in the above equation is also an input
1. > So this gradient means that we’ll move in the x direction by amount $$\frac {\partial f(x,y)}{\partial x}$$ and in the y direction by amount $$\frac {\partial f(x,y)}{\partial y}$$.

---

# 2. Use gradient ascent to find the maximum value of what?

<br />

As we know

> Gradient ascent is based on the idea that if we want to find the maximum point on a function, then the best way to move is in the direction of the gradient.

But, which function's maximum value to find? The answer is [maximum likelihood estimation](https://en.wikipedia.org/wiki/Maximum_likelihood_estimation). And we must first understand the difference between [likelihood](https://en.wikipedia.org/wiki/Likelihood) and [probability](https://en.wikipedia.org/wiki/Probability).

When we find the maximum likelihood estimation, we get the best fit line.

---

# 3. Use gradient descent to find the minimum value of what?

<br />

We can see a lot of pages that use gradient descent to calculate logistic regression. gradient descent is used to find the minimun value of [`loss function`](https://en.wikipedia.org/wiki/Loss_function) or cost function.

If we find the minimum value of loss function, we also get the best fit line.

Here is a great page [Gradient Descent Derivation](http://mccormickml.com/2014/03/04/gradient-descent-derivation/) described gradient descent and loss function in detail.

We can get a loss function by multiple the MLE function with a negative value.

---

# 4. The symbols mean of the gradient ascent algorithm?

$$\omega := \omega + \alpha\Delta_{\omega}f(\omega)$$

<br />

1. MLE (Maximun Likelihood Estimation) is a function which takes the original functions weights as parameters. $$\omega$$ is the parameter vector to calculate MLE here.
1. $$\Delta_{\omega}f(\omega)$$ coresponding to $$\frac {\partial f(\omega)}{\partial {\omega}}$$

---

# 5. How can we get the weights' iteration code?

```python
weights = weights + alpha * dataMatrix.transpose()* error
```

<br />

Let's begin with the sigmoid function equation

$$g(Z) = \frac{1}{1+e^{-Z}} \qquad (1)$$

Construct a linear hypothesis function

$$Z = \theta _0 + \theta _1 x _1 + ... + \theta _n x _n = \sum _{i=0} ^{n} \theta _i x _i = \theta ^{T}x \qquad (2)$$

So

$$h_{\theta}(x) = g(\theta ^{T}x) = \frac {1}{1+e^{-\theta^T x}} \qquad (3)$$

If we use $$h_{\theta}(x)$$ represent the probability of result equals 1, then

$$
P \left(y=1|x;\theta \right) = h_{\theta}(x) \\
\qquad\qquad\quad P \left(y=0|x;\theta \right) = 1 - h_{\theta}(x) \qquad (4)
$$

Equation (4) can be concluded to

$$
P \left(y|x;\theta \right) = \left(h _\theta(x) \right)^y \left(1 - h _\theta(x) \right)^{1-y} \qquad (5)
$$

So we can get the likelihood function

$$
L(\theta) = \prod _{i=1} ^m P \left( y^{(i)} | x^{(i)}; \theta \right) = \prod _{i=1} ^m \left( h _\theta (x^{(i)}) \right) ^{y^{(i)}} \left( 1 - h _\theta (x^{(i)}) \right) ^{1 - y^{(i)}} \qquad (6)
$$

Get the log likelihood function

$$
l(\theta) = log \, L(\theta) = \sum _{i=1} ^m \left( y ^{(i)} log \, h _\theta (x^{(i)}) + (1 - y^{(i)}) log  (1 - h _\theta (x^{(i)})) \right) \qquad (7)
$$

Use gradient ascent to get the maximum value, the iteration is

$$
\theta _j := \theta _j + \alpha \frac {\partial}{\partial \theta _j} l(\theta) \qquad (8)
$$

Refer to [The derivative of $$log_ax$$](http://www.themathpage.com/acalc/exponential.htm#cond), we can get

$$
\frac {\partial}{\partial \theta _j} l(\theta) = \sum _{i=1} ^m \left( y ^{(i)}  \frac {1}{h _\theta (x^{(i)})} \frac {\partial}{\partial \theta _j} h _\theta (x^{(i)}) - (1 - y^{(i)}) \frac {1}{1 - h _\theta (x^{(i)})} \frac {\partial}{\partial \theta _j} h _\theta (x^{(i)}) \right) \\

= \sum _{i=1} ^m \left( y ^{(i)}  \frac {1}{g (\theta ^T x^{(i)})} - (1 - y^{(i)}) \frac {1}{1 - g (\theta ^T x^{(i)})} \right) \frac {\partial}{\partial \theta _j} g (\theta ^T x^{(i)})

\qquad (9)
$$

Refer to [The derivative of $$e^x$$](http://www.themathpage.com/acalc/exponential.htm#expon), we can infer

$$
f(x) = \frac {1}{1 + e ^{g(x)}} \\

\frac {\partial}{\partial x}f(x) = \frac {1}{(1 + e ^{g(x)})^2} e ^{g(x)} \frac {\partial}{\partial x}g(x) \\
= \frac {1}{1 + e ^{g(x)}} \frac {e ^{g(x)}}{1 + e ^{g(x)}} \frac {\partial}{\partial x}g(x) \\
= f(x)(1-f(x)) \frac {\partial}{\partial x}g(x) \qquad (10)
$$

By equation (10), (9) can be infered to

$$
\frac {\partial}{\partial \theta _j} l(\theta)

= \sum _{i=1} ^m \left( y ^{(i)}  \frac {1}{g (\theta ^T x^{(i)})} - (1 - y^{(i)}) \frac {1}{1 - g (\theta ^T x^{(i)})} \right) g (\theta ^T x^{(i)}) (1 - g (\theta ^T x^{(i)})) \frac {\partial}{\partial \theta _j} \theta ^T x^{(i)} \\

= \sum _{i=1} ^m \left( y ^{(i)} (1 - g (\theta ^T x^{(i)})) - (1 - y^{(i)}) g (\theta ^T x^{(i)}) \right) x _j ^{(i)} \\

= \sum _{i=1} ^m \left( y ^{(i)} -  g (\theta ^T x^{(i)}) \right) x _j ^{(i)} \\

= \sum _{i=1} ^m \left( y ^{(i)} -  h _ \theta (x^{(i)}) \right) x _j ^{(i)}

\qquad (11)
$$

Combine equation (8) and (11), and vectorize it, we can get the weights' iteration code.

Here is a refer for the infer process: [Logistic回归总结](http://blog.csdn.net/dongtingzhizi/article/details/15962797)
