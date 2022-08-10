package org.basex.query.func.fn;

import static org.basex.query.func.Function.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.func.file.*;
import org.basex.query.iter.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-22, BSD License
 * @author Christian Gruen
 */
public final class FnTail extends StandardFunc {
  @Override
  public Iter iter(final QueryContext qc) throws QueryException {
    // retrieve and decrement iterator size
    final Iter input = exprs[0].iter(qc);
    final long size = input.size();

    // return empty iterator if iterator yields 0 or 1 items, or if result is an empty sequence
    if(size == 0 || size == 1 || input.next() == null) return Empty.ITER;

    // check if iterator is value-based
    final Value value = input.iterValue();
    if(value != null) return value.subsequence(1, size - 1, qc).iter();

    return new Iter() {
      @Override
      public Item next() throws QueryException {
        return qc.next(input);
      }
      @Override
      public Item get(final long i) throws QueryException {
        return input.get(i + 1);
      }
      @Override
      public long size() {
        return Math.max(-1, size - 1);
      }
    };
  }

  @Override
  public Value value(final QueryContext qc) throws QueryException {
    // return empty sequence if value has 0 or 1 items
    final Value input = exprs[0].value(qc);
    final long size = input.size();
    return size <= 1 ? Empty.VALUE : input.subsequence(1, size - 1, qc);
  }

  @Override
  protected Expr opt(final CompileContext cc) throws QueryException {
    // ignore standard limitation for large values to speed up evaluation of result
    final Expr input = exprs[0];
    if(input instanceof Value) return value(cc.qc);

    final long size = input.size();
    final SeqType st = input.seqType();
    // zero or one result: return empty sequence
    if(size == 0 || size == 1 || st.zeroOrOne()) return Empty.VALUE;
    // two results: return last item
    if(size == 2) return cc.function(_UTIL_LAST, info, input);

    // rewrite nested function calls
    if(TAIL.is(input))
      return cc.function(SUBSEQUENCE, info, input.arg(0), Int.get(3));
    if(SUBSEQUENCE.is(input) || _UTIL_RANGE.is(input)) {
      final SeqRange r = SeqRange.get(input, cc);
      if(r != null) return cc.function(SUBSEQUENCE, info, input.arg(0),
          Int.get(r.start + 2), Int.get(r.length - 1));
    }
    if(REPLICATE.is(input)) {
      final Expr[] args = input.args();
      if(args[1] instanceof Int && args[0].seqType().zeroOrOne()) {
        args[1] = Int.get(((Int) args[1]).itr() - 1);
        return cc.function(REPLICATE, info, args);
      }
    }
    if(_FILE_READ_TEXT_LINES.is(input))
      return FileReadTextLines.opt(this, 1, Long.MAX_VALUE, cc);

    // rewrite list
    if(input instanceof List) {
      final Expr[] args = input.args();
      final Expr first = args[0];
      if(first.seqType().oneOrMore()) {
        // tail((1, 2), 3))  ->  tail(1 to 2), 3
        args[0] = cc.function(TAIL, info, first);
        return List.get(cc, info, args);
      }
    }

    final Expr embedded = embed(cc, false);
    if(embedded != null) return embedded;

    exprType.assign(st.union(Occ.ZERO), size - 1);
    data(input.data());
    return this;
  }

  @Override
  public boolean ddo() {
    return exprs[0].ddo();
  }
}
