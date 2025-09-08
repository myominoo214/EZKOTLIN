import React, { useEffect, useState } from 'react';

const UpdateSlipSingle = (params) => {
  const [number, setNumber] = useState(params.number);
  const [amount, setAmount] = useState(params.amount);

  return (
    <form>
      <h2 className="text-2xl font-semibold mb-4">Update Slip</h2>
      <div className="flex items-center gap-4 mb-4">
        <input
          type="text"
          value={number}
          onChange={(e) => setNumber(e.target.value)}
          placeholder="Number"
          className="flex-1 p-2 border border-gray-300 rounded"
        />
        <input
          type="text"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="Amount"
          className={`flex-1 p-2 border border-gray-300 rounded`}
        />
        <button
          type="button"
          onClick={() => {
            params.handleUpdate(number, amount)
            setNumber('');
            setAmount('');
          }}
          className="p-2 bg-blue-500 text-white rounded hover:bg-blue-600"
        >
          Update
        </button>
      </div>
    </form>
  );
};

export default UpdateSlipSingle;
